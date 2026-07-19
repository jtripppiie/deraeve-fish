package com.tripperdee.deraevfish;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private static final int REQ_NOTIFICATION = 201;
    private static final int RIVER_DARK = Color.rgb(7, 52, 71);
    private static final int RIVER_BLUE = Color.rgb(10, 82, 117);
    private static final int SALMON = Color.rgb(241, 109, 91);
    private static final int ICE = Color.rgb(243, 250, 252);
    private static final int BORDER = Color.rgb(194, 218, 225);
    private static final ZoneId ALASKA = ZoneId.of("America/Anchorage");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private Switch master;
    private Switch sync;
    private Switch wifi;
    private Switch debugMode;
    private Spinner frequency;
    private Spinner quietStart;
    private Spinner quietEnd;
    private RadioGroup modes;
    private EditText thresholdFish;
    private EditText thresholdPercent;
    private EditText fakeAlertTitle;
    private EditText fakeAlertBody;
    private LinearLayout rootLayout;
    private LinearLayout debugPanel;
    private TextView syncInfo;
    private NotificationHelper.TestType pendingTest;
    private boolean pendingCustomTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(RIVER_DARK);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
        prefs = getSharedPreferences("fish_settings", MODE_PRIVATE);
        build();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(ICE);

        LinearLayout root = new LinearLayout(this);
        rootLayout = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ICE);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(21));
        hero.setBackground(gradient(RIVER_DARK, Color.rgb(9, 83, 107)));
        root.addView(hero);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = pill("‹ BACK", Color.WHITE, RIVER_DARK);
        back.setOnClickListener(view -> finish());
        top.addView(back);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(label("Settings", 27, Color.WHITE, true));
        titles.addView(label("Notifications, sync and test tools", 13, Color.rgb(202, 232, 240), false));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMargins(dp(13), 0, 0, 0);
        top.addView(titles, titleParams);
        hero.addView(top);

        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            hero.setPadding(dp(20), bars.top + dp(24), dp(20), dp(24));
            root.setPadding(0, 0, 0, bars.bottom + dp(14));
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(30));
        root.addView(content);

        LinearLayout essentials = card("Notifications & background sync", "Lightweight WorkManager checks run briefly, save valid data, post any new update, and exit.");
        master = toggle("Allow fish-count notifications", prefs.getBoolean("notifications_master", true));
        essentials.addView(master);
        sync = toggle("Automatic background synchronization", prefs.getBoolean("sync_enabled", true));
        essentials.addView(sync);
        wifi = toggle("Use Wi-Fi only", prefs.getBoolean("wifi_only", false));
        essentials.addView(wifi);
        content.addView(essentials, margin(0, 0, 0, 14));

        LinearLayout timing = card("Synchronization timing", "Checks are approximate because Android may defer background work. Each count project still enforces a source-friendly minimum interval.");
        timing.addView(fieldLabel("Check frequency"));
        frequency = new Spinner(this);
        String[] choices = {"Every 6 hours — recommended", "Twice per day — 12 hours", "Once per day — 24 hours", "Every 48 hours"};
        frequency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, choices));
        long savedHours = Math.max(6, prefs.getLong("frequency_hours", 6));
        frequency.setSelection(savedHours == 12 ? 1 : savedHours == 24 ? 2 : savedHours == 48 ? 3 : 0);
        timing.addView(frequency);
        timing.addView(note("Six hours is the most frequent default offered. Manual checks are always available, but repeated source requests inside the minimum interval use saved data."));
        content.addView(timing, margin(0, 0, 0, 14));

        LinearLayout projects = card("Followed count projects", "Only selected projects are checked and shown on the home screen.");
        for (FishRepository.Project project : FishRepository.PROJECTS) {
            boolean defaultFollow = project.id.equals("kenai-sockeye-late") || project.id.equals("kasilof-sockeye");
            CheckBox box = new CheckBox(this);
            box.setText(project.name + "\n" + project.location + " • " + project.run);
            box.setTextColor(RIVER_DARK);
            box.setChecked(prefs.getBoolean("follow_" + project.id, defaultFollow));
            box.setTag(project.id);
            box.setPadding(0, dp(3), 0, dp(3));
            projects.addView(box);
        }
        content.addView(projects, margin(0, 0, 0, 14));

        LinearLayout notificationType = card("Notification type", "Choose how much detail reaches the notification shade.");
        modes = new RadioGroup(this);
        addMode("every", "Every newly reported count");
        addMode("new_dates", "New reporting dates only");
        addMode("significant", "Significant changes only");
        addMode("daily", "One daily summary");
        addMode("none", "No notifications");
        String savedMode = prefs.getString("notification_mode", "every");
        for (int i = 0; i < modes.getChildCount(); i++) {
            RadioButton button = (RadioButton) modes.getChildAt(i);
            if (savedMode.equals(button.getTag())) button.setChecked(true);
        }
        notificationType.addView(modes);
        content.addView(notificationType, margin(0, 0, 0, 14));

        LinearLayout thresholds = card("Significant-change thresholds", "Lower defaults make significant-change mode more responsive while still avoiding tiny corrections.");
        thresholds.addView(fieldLabel("Minimum fish-count change"));
        thresholdFish = input(String.valueOf(prefs.getLong("threshold_fish", 1000L)), "1,000");
        thresholds.addView(thresholdFish);
        thresholds.addView(fieldLabel("Minimum same-date year-over-year change (%)"));
        thresholdPercent = input(String.format(Locale.US, "%.1f", Double.longBitsToDouble(
                prefs.getLong("threshold_percent_bits", Double.doubleToLongBits(5.0)))), "5.0");
        thresholds.addView(thresholdPercent);
        content.addView(thresholds, margin(0, 0, 0, 14));

        LinearLayout quiet = card("Quiet hours", "Updates discovered during quiet hours stay pending for a later background check.");
        LinearLayout quietRow = new LinearLayout(this);
        quietRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout startColumn = spinnerColumn("START", hourSpinner(prefs.getInt("quiet_start", 22)));
        quietStart = (Spinner) startColumn.getChildAt(1);
        LinearLayout endColumn = spinnerColumn("END", hourSpinner(prefs.getInt("quiet_end", 7)));
        quietEnd = (Spinner) endColumn.getChildAt(1);
        quietRow.addView(startColumn, weighted(1, 0, 0, 5, 0));
        quietRow.addView(endColumn, weighted(1, 5, 0, 0, 0));
        quiet.addView(quietRow);
        content.addView(quiet, margin(0, 0, 0, 14));

        LinearLayout diagnostics = card("Status & diagnostics", "Times below are displayed in Alaska time.");
        syncInfo = label("Loading synchronization status…", 13, Color.DKGRAY, false);
        syncInfo.setPadding(0, dp(4), 0, dp(10));
        diagnostics.addView(syncInfo);
        TextView androidSettings = secondaryButton("OPEN ANDROID NOTIFICATION SETTINGS");
        androidSettings.setOnClickListener(view -> openNotificationSettings());
        diagnostics.addView(androidSettings);
        content.addView(diagnostics, margin(0, 0, 0, 14));

        if (prefs.getBoolean("developer_unlocked", false)) {
            LinearLayout developer = card("Secret test alerts", "Unlocked by triple-tapping the fish. These alerts are clearly marked as tests and appear in the normal Android notification shade.");
            debugMode = toggle("Enable test tools", prefs.getBoolean("debug_mode", true));
            developer.addView(debugMode);
            debugPanel = new LinearLayout(this);
            debugPanel.setOrientation(LinearLayout.VERTICAL);

            debugPanel.addView(fieldLabel("Custom alert title"));
            fakeAlertTitle = textInput("Big Kenai push detected", "Alert title", true);
            debugPanel.addView(fakeAlertTitle);
            debugPanel.addView(fieldLabel("Custom alert message"));
            fakeAlertBody = textInput("Fish are moving. Check the newest Kenai count before heading out.", "Alert message", false);
            debugPanel.addView(fakeAlertBody);
            TextView customAlert = testButton("SEND CUSTOM FAKE ALERT");
            customAlert.setOnClickListener(view -> sendCustomTest());
            debugPanel.addView(customAlert, margin(0, 10, 0, 0));

            TextView testUpdate = secondaryButton("SEND TEST NEW-COUNT NOTIFICATION");
            testUpdate.setOnClickListener(view -> sendTest(NotificationHelper.TestType.NEW_COUNT));
            debugPanel.addView(testUpdate, margin(0, 8, 0, 0));
            TextView testRevision = secondaryButton("SEND TEST REVISION NOTIFICATION");
            testRevision.setOnClickListener(view -> sendTest(NotificationHelper.TestType.REVISION));
            debugPanel.addView(testRevision, margin(0, 8, 0, 0));
            TextView testGroup = secondaryButton("SEND TEST GROUPED UPDATES");
            testGroup.setOnClickListener(view -> sendTest(NotificationHelper.TestType.GROUPED));
            debugPanel.addView(testGroup, margin(0, 8, 0, 0));
            debugPanel.setVisibility(debugMode.isChecked() ? View.VISIBLE : View.GONE);
            developer.addView(debugPanel);
            debugMode.setOnCheckedChangeListener((buttonView, isChecked) -> debugPanel.setVisibility(isChecked ? View.VISIBLE : View.GONE));
            content.addView(developer, margin(0, 0, 0, 16));
        }

        TextView save = actionButton("SAVE SETTINGS");
        save.setOnClickListener(view -> save());
        content.addView(save);

        setContentView(scroll);
        loadStatus();
    }

    private void loadStatus() {
        executor.execute(() -> {
            long latestCheck = 0;
            long latestUpdate = 0;
            for (AppDatabase.SyncState state : AppDatabase.get(this).fishDao().allStates()) {
                latestCheck = Math.max(latestCheck, state.lastAttempt);
                latestUpdate = Math.max(latestUpdate, state.lastDetectedUpdate);
            }
            long finalLatestCheck = latestCheck;
            long finalLatestUpdate = latestUpdate;
            runOnUiThread(() -> syncInfo.setText(statusText(finalLatestCheck, finalLatestUpdate)));
        });
    }

    private String statusText(long latestCheck, long latestUpdate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US).withZone(ALASKA);
        String check = latestCheck == 0 ? "Never" : formatter.format(Instant.ofEpochMilli(latestCheck)) + " AK";
        String update = latestUpdate == 0 ? "No post-baseline update detected" : formatter.format(Instant.ofEpochMilli(latestUpdate)) + " AK";
        long hours = Math.max(6, prefs.getLong("frequency_hours", 6));
        String next = latestCheck == 0 ? "Pending first window" : formatter.format(Instant.ofEpochMilli(latestCheck + hours * 60L * 60L * 1000L)) + " AK, approximate";
        return "Last background/manual check: " + check + "\nLast detected official update: " + update + "\nNext approximate window: " + next;
    }

    private void addMode(String value, String text) {
        RadioButton button = new RadioButton(this);
        button.setText(text);
        button.setTextColor(RIVER_DARK);
        button.setTag(value);
        modes.addView(button);
    }

    private void save() {
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean("notifications_master", master.isChecked())
                .putBoolean("sync_enabled", sync.isChecked())
                .putBoolean("wifi_only", wifi.isChecked())
                .putBoolean("debug_mode", debugMode == null ? prefs.getBoolean("debug_mode", false) : debugMode.isChecked())
                .putString("notification_mode", selectedMode())
                .putInt("quiet_start", quietStart.getSelectedItemPosition())
                .putInt("quiet_end", quietEnd.getSelectedItemPosition());
        long[] values = {6, 12, 24, 48};
        editor.putLong("frequency_hours", values[Math.max(0, Math.min(values.length - 1, frequency.getSelectedItemPosition()))]);
        saveCheckBoxes(rootLayout, editor);
        try {
            editor.putLong("threshold_fish", Math.max(0, Long.parseLong(thresholdFish.getText().toString().replace(",", "").trim())));
        } catch (Exception ignored) {
            editor.putLong("threshold_fish", 1000L);
        }
        try {
            editor.putLong("threshold_percent_bits", Double.doubleToLongBits(Math.max(0, Double.parseDouble(thresholdPercent.getText().toString().trim()))));
        } catch (Exception ignored) {
            editor.putLong("threshold_percent_bits", Double.doubleToLongBits(5.0));
        }
        editor.apply();
        FishSyncWorker.schedule(this);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveCheckBoxes(ViewGroup group, SharedPreferences.Editor editor) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof CheckBox box && box.getTag() instanceof String id) {
                editor.putBoolean("follow_" + id, box.isChecked());
            } else if (child instanceof ViewGroup nested) {
                saveCheckBoxes(nested, editor);
            }
        }
    }

    private String selectedMode() {
        int id = modes.getCheckedRadioButtonId();
        RadioButton selected = id == -1 ? null : findViewById(id);
        return selected == null ? "every" : String.valueOf(selected.getTag());
    }

    private void sendTest(NotificationHelper.TestType type) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingTest = type;
            pendingCustomTest = false;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
            return;
        }
        NotificationHelper.sendTestNotification(this, type);
        Toast.makeText(this, "Test notification sent to the notification shade", Toast.LENGTH_LONG).show();
    }

    private void sendCustomTest() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingTest = null;
            pendingCustomTest = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
            return;
        }
        String title = fakeAlertTitle == null ? "Fish alert" : fakeAlertTitle.getText().toString().trim();
        String body = fakeAlertBody == null ? "This is a simulated DeRaeve notification." : fakeAlertBody.getText().toString().trim();
        NotificationHelper.sendCustomTestNotification(this, title, body);
        Toast.makeText(this, "Custom fake alert sent to the notification shade", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingTest != null) {
                NotificationHelper.TestType type = pendingTest;
                pendingTest = null;
                sendTest(type);
            } else if (pendingCustomTest) {
                pendingCustomTest = false;
                sendCustomTest();
            }
        }
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, FishApplication.CHANNEL_COUNTS);
        startActivity(intent);
    }

    private LinearLayout card(String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(16));
        card.setBackground(rounded(Color.WHITE, dp(18), BORDER));
        card.setElevation(dp(1));
        card.addView(label(title, 18, RIVER_DARK, true));
        card.addView(label(subtitle, 12, Color.DKGRAY, false), margin(0, 4, 0, 9));
        return card;
    }

    private Switch toggle(String text, boolean checked) {
        Switch value = new Switch(this);
        value.setText(text);
        value.setTextColor(RIVER_DARK);
        value.setTextSize(14);
        value.setChecked(checked);
        value.setPadding(0, dp(4), 0, dp(4));
        return value;
    }

    private TextView actionButton(String value) {
        TextView button = label(value, 14, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(15), dp(14), dp(15));
        button.setBackground(rounded(SALMON, dp(14), SALMON));
        button.setElevation(dp(2));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView testButton(String value) {
        TextView button = label(value, 12, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(13), dp(12), dp(13));
        button.setBackground(rounded(RIVER_BLUE, dp(12), RIVER_BLUE));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView secondaryButton(String value) {
        TextView button = label(value, 12, RIVER_DARK, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackground(rounded(Color.rgb(236, 247, 249), dp(12), Color.rgb(197, 224, 231)));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView pill(String value, int fill, int color) {
        TextView pill = label(value, 11, color, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(9), dp(12), dp(9));
        pill.setBackground(rounded(fill, dp(20), fill));
        pill.setClickable(true);
        pill.setFocusable(true);
        return pill;
    }

    private TextView fieldLabel(String text) {
        TextView value = label(text, 12, RIVER_DARK, true);
        value.setPadding(0, dp(8), 0, dp(2));
        return value;
    }

    private TextView note(String text) {
        TextView value = label(text, 12, Color.DKGRAY, false);
        value.setPadding(0, dp(6), 0, 0);
        return value;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView value = new TextView(this);
        value.setText(text);
        value.setTextSize(size);
        value.setTextColor(color);
        value.setLineSpacing(0, 1.08f);
        if (bold) value.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return value;
    }


    private EditText textInput(String value, String hint, boolean singleLine) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setTextColor(RIVER_DARK);
        input.setHintTextColor(Color.GRAY);
        input.setSingleLine(singleLine);
        input.setMinLines(singleLine ? 1 : 3);
        input.setMaxLines(singleLine ? 1 : 5);
        input.setGravity(singleLine ? Gravity.CENTER_VERTICAL : Gravity.TOP);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                (singleLine ? android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES : android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        input.setBackground(rounded(Color.rgb(248, 252, 253), dp(10), Color.rgb(204, 225, 231)));
        input.setPadding(dp(12), dp(11), dp(12), dp(11));
        return input;
    }

    private EditText input(String value, String hint) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setTextColor(RIVER_DARK);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setBackground(rounded(Color.rgb(248, 252, 253), dp(10), Color.rgb(204, 225, 231)));
        input.setPadding(dp(12), dp(11), dp(12), dp(11));
        return input;
    }

    private Spinner hourSpinner(int selected) {
        Spinner spinner = new Spinner(this);
        String[] values = new String[24];
        for (int hour = 0; hour < 24; hour++) {
            int display = hour % 12 == 0 ? 12 : hour % 12;
            values[hour] = display + (hour < 12 ? " AM" : " PM");
        }
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(Math.max(0, Math.min(23, selected)));
        return spinner;
    }

    private LinearLayout spinnerColumn(String title, Spinner spinner) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(label(title, 10, Color.GRAY, true));
        column.addView(spinner);
        return column;
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams weighted(float weight, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable gradient(int start, int end) {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
