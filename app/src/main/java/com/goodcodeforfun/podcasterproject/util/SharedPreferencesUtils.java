package com.goodcodeforfun.podcasterproject.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.goodcodeforfun.podcasterproject.R;

public class SharedPreferencesUtils {
    private static String IS_HIDE_IMAGES_KEY;
    private static SharedPreferences prefs;

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
}
