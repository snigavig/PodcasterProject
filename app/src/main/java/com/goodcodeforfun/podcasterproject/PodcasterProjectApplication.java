package com.goodcodeforfun.podcasterproject;

import com.facebook.stetho.Stetho;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.goodcodeforfun.podcasterproject.sync.SyncManager;
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
        SyncManager.startSyncPodcastsTask();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public SharedPreferencesUtils getSharedPreferencesUtils() {
        return mSharedPreferencesUtils;
    }

}