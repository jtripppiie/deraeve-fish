package com.tripperdee.deraevfish;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;

import java.util.List;

@Database(
        entities = {
                AppDatabase.CountRecord.class,
                AppDatabase.SyncState.class,
                AppDatabase.Announcement.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    public static final java.util.concurrent.ExecutorService databaseWriteExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static volatile AppDatabase INSTANCE;

    public abstract FishDao fishDao();

    public static AppDatabase get(Context context) {
        AppDatabase current = INSTANCE;
        if (current == null) {
            synchronized (AppDatabase.class) {
                current = INSTANCE;
                if (current == null) {
                    current = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "fish-count.db"
                            )
                            .build();
                    INSTANCE = current;
                }
            }
        }
        return current;
    }

    @Entity(
            tableName = "count_records",
            primaryKeys = {"project_id", "report_date"},
            indices = {@Index(value = {"project_id", "year"})}
    )
    public static class CountRecord {
        @NonNull @ColumnInfo(name = "project_id") public String projectId = "";
        @NonNull @ColumnInfo(name = "report_date") public String reportDate = "";
        public int year;
        @ColumnInfo(name = "daily_count") public long dailyCount;
        @ColumnInfo(name = "cumulative_count") public long cumulativeCount;
        public String notes;
        public String status;
        @ColumnInfo(name = "official_update_time") public String officialUpdateTime;
        @ColumnInfo(name = "retrieved_at") public long retrievedAt;
        public String fingerprint;
    }

    @Entity(tableName = "sync_state")
    public static class SyncState {
        @PrimaryKey @NonNull @ColumnInfo(name = "project_id") public String projectId = "";
        public String etag;
        @ColumnInfo(name = "last_modified") public String lastModified;
        @ColumnInfo(name = "last_attempt") public long lastAttempt;
        @ColumnInfo(name = "last_success") public long lastSuccess;
        @ColumnInfo(name = "last_detected_update") public long lastDetectedUpdate;
        @ColumnInfo(name = "failure_count") public int failureCount;
        @ColumnInfo(name = "breaker_until") public long breakerUntil;
        @ColumnInfo(name = "last_error") public String lastError;
        @ColumnInfo(name = "latest_fingerprint") public String latestFingerprint;
    }

    @Entity(
            tableName = "announcements",
            indices = {@Index(value = {"announcement_key"}, unique = true)}
    )
    public static class Announcement {
        @PrimaryKey(autoGenerate = true) public long id;
        @NonNull @ColumnInfo(name = "announcement_key") public String announcementKey = "";
        @NonNull @ColumnInfo(name = "project_id") public String projectId = "";
        @NonNull public String type = "NONE";
        @NonNull @ColumnInfo(name = "report_date") public String reportDate = "";
        @ColumnInfo(name = "daily_count") public long dailyCount;
        @ColumnInfo(name = "cumulative_count") public long cumulativeCount;
        @ColumnInfo(name = "previous_daily") public long previousDaily;
        @ColumnInfo(name = "previous_cumulative") public long previousCumulative;
        @ColumnInfo(name = "yoy_percent") public Double yoyPercent;
        @ColumnInfo(name = "five_year_average") public Double fiveYearAverage;
        @NonNull @ColumnInfo(name = "delivery_state") public String deliveryState = "PENDING";
        @ColumnInfo(name = "created_at") public long createdAt;
    }

    @Dao
    public interface FishDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsertRecords(List<CountRecord> records);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsertState(SyncState state);

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long insertAnnouncement(Announcement announcement);

        @Query("SELECT * FROM count_records WHERE project_id = :projectId ORDER BY report_date DESC LIMIT 1")
        CountRecord latest(String projectId);

        @Query("SELECT * FROM count_records WHERE project_id = :projectId AND report_date = :date LIMIT 1")
        CountRecord byDate(String projectId, String date);

        @Query("SELECT * FROM count_records WHERE project_id = :projectId ORDER BY report_date DESC LIMIT :limit")
        List<CountRecord> recent(String projectId, int limit);

        @Query("SELECT * FROM count_records WHERE project_id = :projectId AND year = :year ORDER BY report_date ASC")
        List<CountRecord> recordsForYear(String projectId, int year);

        @Query("SELECT * FROM count_records WHERE project_id = :projectId AND year = :year AND substr(report_date, 6) = :monthDay LIMIT 1")
        CountRecord sameMonthDay(String projectId, int year, String monthDay);

        @Query("SELECT AVG(daily_count) FROM count_records WHERE project_id = :projectId AND year BETWEEN :startYear AND :endYear AND substr(report_date, 6) = :monthDay")
        Double averageForDay(String projectId, int startYear, int endYear, String monthDay);

        @Query("SELECT COUNT(*) FROM count_records WHERE project_id = :projectId")
        int recordCount(String projectId);

        @Query("SELECT * FROM sync_state WHERE project_id = :projectId LIMIT 1")
        SyncState state(String projectId);

        @Query("SELECT * FROM sync_state ORDER BY last_attempt DESC")
        List<SyncState> allStates();

        @Query("SELECT * FROM announcements WHERE delivery_state = 'PENDING' ORDER BY created_at ASC")
        List<Announcement> pendingAnnouncements();

        @Query("UPDATE announcements SET delivery_state = :state WHERE id IN (:ids)")
        void setAnnouncementState(List<Long> ids, String state);

        @Query("UPDATE announcements SET delivery_state = 'SUPPRESSED' WHERE project_id = :projectId AND delivery_state = 'PENDING'")
        void suppressProject(String projectId);
    }
}
