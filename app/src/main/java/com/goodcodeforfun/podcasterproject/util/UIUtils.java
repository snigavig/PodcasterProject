package com.goodcodeforfun.podcasterproject.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.goodcodeforfun.podcasterproject.R;

/**
 * Created by snigavig on 02.01.17.
 */

public class UIUtils {
    private static final int FALLBACK_ACTIONBAR_HEIGHT = 48; //from current guidelines

    public static void setViewMargins(View v, int left, int top, int right, int bottom) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }

    public static void setViewHeight(View v, int h) {
        if (v.getLayoutParams() != null) {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            p.height = h;
            v.requestLayout();
        }
    }

    public static int getSeekBarHeight(Context context) {
        Resources resources = context.getResources();
        return (int) ((int) resources.getDimension(R.dimen.seek_bar_height) / resources.getDisplayMetrics().density);
    }

    public static int getFabSize(Context context) {
        Resources resources = context.getResources();
        return (int) ((int) resources.getDimension(R.dimen.fab_size) / resources.getDisplayMetrics().density);
    }

    public static int getFabMargin(Context context) {
        Resources resources = context.getResources();
        return (int) ((int) resources.getDimension(R.dimen.fab_margin) / resources.getDisplayMetrics().density);
    }

    public static int getActionBarSize(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return FALLBACK_ACTIONBAR_HEIGHT;
    }
}
