package com.goodcodeforfun.podcasterproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.goodcodeforfun.stateui.StateUIActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends StateUIActivity {

    private static final String TAG = "MainActivity";
    private static final int RC_PLAY_SERVICES = 123;
    private static final int FALLBACK_ACTIONBAR_HEIGHT = 48; //from current guidelines

    private static DisplayMetrics displayMetrics;

    private AppCompatTextView marqueueTitle;

    private FloatingActionButton fabPlayPause;
    private FloatingActionButton fabPrevious;
    private FloatingActionButton fabNext;
    private Animation growAnimation;
    private AppBarLayout mActionBar;
    private AppCompatSeekBar seekBarProgress;
    private RecyclerView podcastsRecyclerView;
    private AppCompatImageView podcastBigImageView;
    private AppCompatTextView podcastCurrentTime;

    private BroadcastReceiver mReceiver;
    private Podcast currentPodcast;

    private static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    private static void setHeight(View v, int h) {
        if (v.getLayoutParams() != null) {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            p.height = h;
            v.requestLayout();
        }
    }

    private static int getSeekBarHeight(Context context) {
        return (int) ((int) context.getResources().getDimension(R.dimen.seek_bar_height) / displayMetrics.density);
    }

    private static int getFabSize(Context context) {
        return (int) ((int) context.getResources().getDimension(R.dimen.fab_size) / displayMetrics.density);
    }

    private static int getFabMargin(Context context) {
        return (int) ((int) context.getResources().getDimension(R.dimen.fab_margin) / displayMetrics.density);
    }

    private static int getActionBarSize(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return FALLBACK_ACTIONBAR_HEIGHT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepareUI();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(SyncTasksService.ACTION_DONE)) {
                    String tag = intent.getStringExtra(SyncTasksService.EXTRA_TAG);
                    int result = intent.getIntExtra(SyncTasksService.EXTRA_RESULT, -1);

                    String msg = String.format("DONE: %s (%d)", tag, result);
                    showSimpleToast(msg);
                }
            }
        };

        checkPlayServicesAvailable();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SyncTasksService.ACTION_DONE);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mReceiver);
    }

    private void checkPlayServicesAvailable() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int resultCode = availability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(resultCode)) {
                // Show dialog to resolve the error.
                availability.getErrorDialog(this, resultCode, RC_PLAY_SERVICES).show();
            } else {
                // Unresolvable error
                showSimpleToast("Google Play Services error");
            }
        }
    }

    @Override
    protected void onNoDataUI() {

    }

    @Override
    public void onSuccessUI() {
        showSimpleToast("Success");
    }

    @Override
    public void onErrorUI() {
        showSimpleToast("Error");
    }

    private void initDetailsPanel() {
        if (marqueueTitle != null && currentPodcast != null) {
            marqueueTitle.setText(currentPodcast.getTitle());

            Glide.with(this)
                    .load(currentPodcast.getImageUrl())
                    .placeholder(R.color.colorPrimaryHalfTransparent)
                    .into(podcastBigImageView);
        }
    }

    private void prepareUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActionBar = (AppBarLayout) findViewById(R.id.toolbarLayout);

        fabPlayPause = (FloatingActionButton) findViewById(R.id.play_pause_button);
        fabPrevious = (FloatingActionButton) findViewById(R.id.previous_track_button);
        fabNext = (FloatingActionButton) findViewById(R.id.next_track_button);

        fabPrevious.setClickable(false);
        fabNext.setClickable(false);

        final View actionBarStabView = findViewById(R.id.action_bar_stab_view);
        final float dpWidth;
        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_coordinator);
        final View bottomSheet = coordinatorLayout.findViewById(R.id.details_bottom_sheet);

        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);


        final FrameLayout detailsWrap = (FrameLayout) findViewById(R.id.detailsWrap);
        final LinearLayout visiblePart = (LinearLayout) detailsWrap.findViewById(R.id.visiblePart);
        podcastBigImageView = (AppCompatImageView) detailsWrap.findViewById(R.id.podcastBigImageView);

        marqueueTitle = (AppCompatTextView) detailsWrap.findViewById(R.id.marqueeTitle);
        podcastCurrentTime = (AppCompatTextView) detailsWrap.findViewById(R.id.podcastCurrentTimeTextView);

        growAnimation = AnimationUtils.loadAnimation(this, R.anim.simple_grow);

        displayMetrics = getResources().getDisplayMetrics();
        dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        int shadowHeight = (int) (getResources().getDimension(R.dimen.bottom_sheet_shadow) / displayMetrics.density);

        final int visiblePartHeight = (int) (getFabSize(this) * displayMetrics.density) +
                (int) (getFabMargin(this) * displayMetrics.density * 2) +
                (int) (shadowHeight * displayMetrics.density * 2);

        setHeight(visiblePart, visiblePartHeight);


        setMargins(detailsWrap, 0, 0, 0,
                (int) (getFabSize(this) * displayMetrics.density) +
                        (int) (getSeekBarHeight(this) * displayMetrics.density * 2) +
                        (int) (getFabMargin(this) * displayMetrics.density * 2) +
                        (int) (shadowHeight * displayMetrics.density));

        seekBarProgress = (AppCompatSeekBar) findViewById(R.id.seekBarProgress);
        seekBarProgress.setMax(99);

        podcastsRecyclerView = (RecyclerView) findViewById(R.id.podcast_track_list);
        podcastsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        podcastsRecyclerView.setLayoutManager(mLayoutManager);


        final int actionBarSize = getActionBarSize(this);
        final int actionBarStabSize = actionBarSize
                - (int) getResources().getDimension(R.dimen.bottom_sheet_shadow)
                - 1;

        actionBarStabView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, actionBarStabSize));


        int fabElevation = (int) (getResources().getDimension(R.dimen.fab_elevation) / displayMetrics.density);

        int fabElevationCompensation = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setMargins(fabPlayPause, 0, 0, 0, 0);
            fabElevationCompensation = (int) (fabElevation * displayMetrics.density * 2);
        }


        behavior.setPeekHeight(
                (int) (getFabSize(this) * displayMetrics.density)
                        + (int) (getFabMargin(this) * displayMetrics.density * 2)
                        + actionBarStabSize
                        + (int) (shadowHeight * displayMetrics.density * 2)
                        + fabElevationCompensation);


        setMargins(podcastsRecyclerView, 0, 0, 0,
                (int) (getFabSize(this) * displayMetrics.density) +
                        (int) (getFabMargin(this) * displayMetrics.density * 2) +
                        (int) (shadowHeight * displayMetrics.density));


        setMargins(marqueueTitle, 0, 0,
                (int) ((dpWidth * displayMetrics.density) / 3), 0);

        fabPlayPause.setTranslationX((dpWidth * displayMetrics.density) / 3);

        initDetailsPanel();

        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                switch (newState) {

                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        fabPrevious.setClickable(false);
                        fabNext.setClickable(false);
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        fabPrevious.setClickable(true);
                        fabNext.setClickable(true);
                        break;
                }

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float slideOffsetNegative = (1 - slideOffset);
                float slideOffsetEasingCoefficient = (10 * slideOffsetNegative);
                float slideOffsetEased = slideOffset / slideOffsetEasingCoefficient;
                fabPlayPause.setTranslationX(((dpWidth * displayMetrics.density) / 3) * slideOffsetNegative);
                fabNext.setAlpha(slideOffsetEased);
                fabPrevious.setAlpha(slideOffsetEased);
                seekBarProgress.setAlpha(slideOffsetEased);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mActionBar.setElevation(displayMetrics.density * (8 - (slideOffset * 8)));
                }
                podcastsRecyclerView.setAlpha(slideOffsetNegative);

                setMargins(marqueueTitle, 0, 0,
                        (int) (((dpWidth * displayMetrics.density) / 3) * slideOffsetNegative), 0);
            }
        });
    }
}
