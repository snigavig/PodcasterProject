package com.goodcodeforfun.podcasterproject;

import android.util.Log;

import com.facebook.stetho.Stetho;
import com.goodcodeforfun.stateui.StateUIApplication;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;

import io.realm.Realm;

/**
 * Created by snigavig on 30.12.16.
 */

public class PodcasterProjectApplication extends StateUIApplication {
    private static final String TAG = PodcasterProjectApplication.class.getSimpleName();
    private GcmNetworkManager mGcmNetworkManager;

    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        Stetho.initializeWithDefaults(this);
        mGcmNetworkManager = GcmNetworkManager.getInstance(this);
        startSyncPodcastsTask();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onTerminate() {
        super.onTerminate();
        // a way to cancel all tasks
        //mGcmNetworkManager.cancelAllTasks(SyncTasksService.class);
    }

    public void startSyncPodcastsTask() {
        Log.d(TAG, "startSyncPodcastsTask");
        PeriodicTask task = new PeriodicTask.Builder()
                .setService(SyncTasksService.class)
                .setTag(SyncTasksService.TASK_TAG_SYNC_PODCASTS)
                .setPeriod(30L)
                .build();

        mGcmNetworkManager.schedule(task);
    }

    public void stopSyncPodcastsTask() {
        Log.d(TAG, "stopSyncPodcastsTask");
        mGcmNetworkManager.cancelTask(SyncTasksService.TASK_TAG_SYNC_PODCASTS, SyncTasksService.class);
    }

}