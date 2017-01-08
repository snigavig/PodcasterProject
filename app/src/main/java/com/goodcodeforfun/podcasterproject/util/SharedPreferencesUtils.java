package com.goodcodeforfun.podcasterproject.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.goodcodeforfun.podcasterproject.PlayerService;
import com.goodcodeforfun.podcasterproject.R;

public class SharedPreferencesUtils {
    private static final String LAST_STATE_KEY = "LAST_STATE";
    private static final String LAST_PODCAST_KEY = "LAST_PODCAST";
    private static final String LAST_PODCAST_TIME_KEY = "LAST_PODCAST_TIME";
    private final String IS_HIDE_IMAGES_KEY;
    private final SharedPreferences prefs;

    public SharedPreferencesUtils(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        IS_HIDE_IMAGES_KEY = context.getString(R.string.show_podcast_images_key);
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    @SuppressLint("CommitPrefEdits")
    public void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear().commit();
    }

    public boolean isHideImages() {
        return prefs.getBoolean(IS_HIDE_IMAGES_KEY, false);
    }

    public void setHideImages(boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_HIDE_IMAGES_KEY, value);
        editor.apply();
    }


    public int getLastState() {
        return prefs.getInt(LAST_STATE_KEY, PlayerService.PAUSED);
    }

    public void setLastState(@PlayerService.PlayerState int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(LAST_STATE_KEY, value);
        editor.apply();
    }

    public String getLastPodcast() {
        return prefs.getString(LAST_PODCAST_KEY, "");
    }

    public void setLastPodcast(String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_PODCAST_KEY, value);
        editor.apply();
    }

    public int getLastPodcastTime() {
        return prefs.getInt(LAST_PODCAST_TIME_KEY, -1);
    }

    public void setLastPodcastTime(int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(LAST_PODCAST_TIME_KEY, value);
        editor.apply();
    }
}
