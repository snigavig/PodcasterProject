package com.goodcodeforfun.podcasterproject.sync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Trigger;
import com.goodcodeforfun.podcasterproject.BuildConfig;
import com.goodcodeforfun.podcasterproject.PodcasterProjectApplication;

import okhttp3.OkHttpClient;

public class SyncManager extends IntentService {
    private static final String TAG = SyncManager.class.getSimpleName();
    private static final String ACTION_SYNC_PODCASTS_IMMEDIATELY = "SyncManager#ACTION_SYNC_PODCASTS_IMMEDIATELY";

    public SyncManager() {
        super("SyncManager");
    }

    public static void startActionSyncPodcastsImmediately(Context context) {
        Intent intent = new Intent(context, SyncManager.class);
        intent.setAction(ACTION_SYNC_PODCASTS_IMMEDIATELY);
        context.startService(intent);
    }

    public static void startSyncPodcastsTask() {
        Log.d(TAG, "startSyncPodcastsTask");
        Job task = PodcasterProjectApplication.getInstance().getFirebaseJobDispatcher().newJobBuilder()
                .setService(SyncTasksService.class)
                .setTag(SyncTasksService.TASK_TAG_SYNC_PODCASTS)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(0, 30))
                .build();

        PodcasterProjectApplication.getInstance().getFirebaseJobDispatcher().schedule(task);
    }

    public static void stopSyncPodcastsTask() {
        Log.d(TAG, "stopSyncPodcastsTask");
        PodcasterProjectApplication.getInstance().getFirebaseJobDispatcher().cancel(SyncTasksService.TASK_TAG_SYNC_PODCASTS);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC_PODCASTS_IMMEDIATELY.equals(action)) {
                syncPodcastsImmediately(SyncManager.this);
            }
        }
    }

    private void syncPodcastsImmediately(Context context) {
        OkHttpClient client = new OkHttpClient();
        String url = BuildConfig.PODCAST_URL;
        if (!"".equals(url)) {
            @JobService.JobResult int result = SyncTasksService.processRss(client, url);
            SyncTasksService.sendResultBroadcast(context, SyncTasksService.TASK_TAG_INITIAL_SYNC_PODCASTS, result);
        }
    }
}

