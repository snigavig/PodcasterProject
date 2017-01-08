package com.goodcodeforfun.podcasterproject.util;

/**
 * Created by snigavig on 06.01.17.
 */

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Usage:
 * <p/>
 * 1. Get the Foreground Singleton, passing a Context or Application object unless you
 * are sure that the Singleton has definitely already been initialised elsewhere.
 * <p/>
 * 2.a) Perform a direct, synchronous check: Foreground.isForeground() / .isBackground()
 * <p/>
 * or
 * <p/>
 * 2.b) Register to be notified (useful in Service or other non-UI components):
 * <p/>
 * Foreground.Listener myListener = new Foreground.Listener(){
 * public void onBecameForeground(){
 * // ... whatever you want to do
 * }
 * public void onBecameBackground(){
 * // ... whatever you want to do
 * }
 * }
 * <p/>
 * public void onCreate(){
 * super.onCreate();
 * Foreground.get(this).addListener(listener);
 * }
 * <p/>
 * public void onDestroy(){
 * super.onCreate();
 * Foreground.get(this).removeListener(listener);
 * }
 */
public class Foreground implements Application.ActivityLifecycleCallbacks {

    private static final long CHECK_DELAY = 500;
    private static final String TAG = Foreground.class.getName();
    private static Foreground instance;
    private final Handler handler = new Handler();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private boolean foreground = false, paused = true;
    private Runnable check;

    /**
     * Its not strictly necessary to use this method - _usually_ invoking
     * get with a Context gives us a path to retrieve the Application and
     * initialise, but sometimes (e.g. in test harness) the ApplicationContext
     * is != the Application, and the docs make no guarantees.
     *
     * @param application
     * @return an initialised Foreground instance
     */
    public static Foreground init(Application application) {
        if (instance == null) {
            instance = new Foreground();
            application.registerActivityLifecycleCallbacks(instance);
        }
        return instance;
    }

    public static Foreground get(Application application) {
        if (instance == null) {
            init(application);
        }
        return instance;
    }

    public static Foreground get(Context ctx) {
        if (instance == null) {
            Context appCtx = ctx.getApplicationContext();
            if (appCtx instanceof Application) {
                init((Application) appCtx);
            }
            throw new IllegalStateException(
                    "Foreground is not initialised and " +
                            "cannot obtain the Application object");
        }
        return instance;
    }

    public static Foreground get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Foreground is not initialised - invoke " +
                            "at least once with parameterised init/get");
        }
        return instance;
    }

    public boolean isForeground() {
        return foreground;
    }

    public boolean isBackground() {
        return !foreground;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);

        if (wasBackground) {
            Log.d(TAG, "went foreground");
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception exc) {
                    Log.e(TAG, exc.getLocalizedMessage());
                }
            }
        } else {
            Log.d(TAG, "still foreground");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = new Runnable() {
            @Override
            public void run() {
                if (foreground && paused) {
                    foreground = false;
                    Log.d(TAG, "went background");
                    for (Listener l : listeners) {
                        try {
                            l.onBecameBackground();
                        } catch (Exception exc) {
                            Log.e(TAG, exc.getLocalizedMessage());
                        }
                    }
                } else {
                    Log.d(TAG, "still foreground");
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // This is intentionally empty
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // This is intentionally empty
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // This is intentionally empty
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // This is intentionally empty
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // This is intentionally empty
    }

    public interface Listener {

        void onBecameForeground();

        void onBecameBackground();

    }
}