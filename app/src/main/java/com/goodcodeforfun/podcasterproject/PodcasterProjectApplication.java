package com.goodcodeforfun.podcasterproject;

import android.util.Log;

import com.facebook.stetho.Stetho;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.goodcodeforfun.podcasterproject.sync.SyncTasksService;
import com.goodcodeforfun.podcasterproject.util.Foreground;
import com.goodcodeforfun.podcasterproject.util.SharedPreferencesUtils;
import com.goodcodeforfun.stateui.StateUIApplication;

import io.realm.Realm;

/**
 * Created by snigavig on 30.12.16.
 */

public class PodcasterProjectApplication extends StateUIApplication {
    private static final String TAG = PodcasterProjectApplication.class.getSimpleName();

    private static PodcasterProjectApplication mInstance;
    private FirebaseJobDispatcher mFirebaseJobDispatcher;
    private SharedPreferencesUtils mSharedPreferencesUtils;

    public static PodcasterProjectApplication getInstance() {
        return mInstance;
    }

    public FirebaseJobDispatcher getFirebaseJobDispatcher() {
        return mFirebaseJobDispatcher;
    }

    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Foreground.init(this);
        mSharedPreferencesUtils = new SharedPreferencesUtils(this);
        Realm.init(this);
        Stetho.initializeWithDefaults(this);
        mFirebaseJobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        startSyncPodcastsTask();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public SharedPreferencesUtils getSharedPreferencesUtils() {
        return mSharedPreferencesUtils;
    }

    private void startSyncPodcastsTask() {
        Log.d(TAG, "startSyncPodcastsTask");
        Job task = getFirebaseJobDispatcher().newJobBuilder()
                .setService(SyncTasksService.class)
                .setTag(SyncTasksService.TASK_TAG_SYNC_PODCASTS)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(0, 3600))
                .build();

        mFirebaseJobDispatcher.schedule(task);
    }

    public void stopSyncPodcastsTask() {
        Log.d(TAG, "stopSyncPodcastsTask");
        mFirebaseJobDispatcher.cancel(SyncTasksService.TASK_TAG_SYNC_PODCASTS);
    }
}