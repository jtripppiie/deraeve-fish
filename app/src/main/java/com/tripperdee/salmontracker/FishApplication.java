package com.tripperdee.salmontracker;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

public class FishApplication extends Application {
    public static final String CHANNEL_COUNTS = "salmontracker_fish_count_updates_v2";
    public static final String CHANNEL_SOURCE = "salmontracker_source_status";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels(this);
        FishSyncWorker.schedule(this);
    }

    public static void createChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel counts = new NotificationChannel(
                CHANNEL_COUNTS,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        counts.setDescription(context.getString(R.string.notification_channel_description));
        counts.enableVibration(true);
        counts.enableLights(true);
        counts.setLightColor(Color.rgb(241, 109, 91));
        counts.setShowBadge(true);
        manager.createNotificationChannel(counts);

        NotificationChannel source = new NotificationChannel(
                CHANNEL_SOURCE,
                "Salmon Tracker Source Status",
                NotificationManager.IMPORTANCE_LOW
        );
        source.setDescription("Quiet warnings when the official source repeatedly fails or changes format.");
        manager.createNotificationChannel(source);
    }
}
