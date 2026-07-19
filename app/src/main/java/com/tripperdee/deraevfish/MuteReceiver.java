package com.tripperdee.deraevfish;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MuteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String projectId = intent.getStringExtra("project_id");
        if (projectId == null || projectId.isBlank()) return;
        context.getSharedPreferences("fish_settings", Context.MODE_PRIVATE)
                .edit().putBoolean("follow_" + projectId, false).apply();
        AppDatabase.databaseWriteExecutor.execute(() ->
                AppDatabase.get(context).fishDao().suppressProject(projectId));
        Toast.makeText(context, "Muted this fish-count project", Toast.LENGTH_SHORT).show();
        FishSyncWorker.schedule(context);
    }
}
