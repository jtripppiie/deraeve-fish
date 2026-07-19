package com.tripperdee.deraevfish;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FishSyncWorker extends Worker {
    public static final String UNIQUE_WORK = "deraev-fish-periodic-sync";

    public FishSyncWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("fish_settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("sync_enabled", true)) return Result.success();
        if (!FishLogic.isActiveSeason(LocalDate.now(ZoneId.of("America/Anchorage")))) return Result.success();

        FishRepository repository = new FishRepository(context);
        List<FishRepository.SyncResult> results = new ArrayList<>();
        boolean temporaryFailure = false;
        for (FishRepository.Project project : repository.followedProjects()) {
            FishRepository.SyncResult result = repository.syncProject(project, false);
            results.add(result);
            temporaryFailure |= result.sourceFailure && !result.breakerOpened;
        }
        NotificationHelper.dispatch(context, results);
        return temporaryFailure ? Result.retry() : Result.success();
    }

    public static void schedule(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("fish_settings", Context.MODE_PRIVATE);
        WorkManager manager = WorkManager.getInstance(context);
        if (!prefs.getBoolean("sync_enabled", true)) {
            manager.cancelUniqueWork(UNIQUE_WORK);
            return;
        }
        long requested = prefs.getLong("frequency_hours", 6L);
        long hours = Math.max(6L, requested);
        boolean wifiOnly = prefs.getBoolean("wifi_only", false);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(wifiOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(FishSyncWorker.class, hours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag("fish-count-sync")
                .build();
        manager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request);
    }
}
