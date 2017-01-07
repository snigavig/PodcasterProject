package com.goodcodeforfun.podcasterproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.bumptech.glide.Glide;
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.sync.SyncTasksService;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;
import com.goodcodeforfun.podcasterproject.util.UIUtils;
import com.goodcodeforfun.stateui.StateUIActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

import static com.goodcodeforfun.podcasterproject.PlayerService.NEXT_ACTION_KEY;
import static com.goodcodeforfun.podcasterproject.PlayerService.PAUSE_ACTION_KEY;
import static com.goodcodeforfun.podcasterproject.PlayerService.PLAY_ACTION_KEY;
import static com.goodcodeforfun.podcasterproject.PlayerService.PREVIOUS_ACTION_KEY;

public class MainActivity extends StateUIActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MainActivity";
    private static final int RC_PLAY_SERVICES = 123;
    private static int index = -1;
    private static int top = -1;
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
    private LinearLayoutManager mLayoutManager;
    private Podcast currentPodcast;
    private PodcastListAdapter mAdapter;

    private BroadcastReceiver mSyncStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SyncTasksService.ACTION_DONE)) {
                String tag = intent.getStringExtra(SyncTasksService.EXTRA_TAG);
                int result = intent.getIntExtra(SyncTasksService.EXTRA_RESULT, -1);

                String msg = String.format(Locale.getDefault(), "DONE: %s (%d)", tag, result);
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                showSimpleToast(msg);
            }
        }
    };

    private BroadcastReceiver mPlayerStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PLAY_ACTION_KEY:
                    break;
                case PAUSE_ACTION_KEY:
                    break;
                case NEXT_ACTION_KEY:
                    break;
                case PREVIOUS_ACTION_KEY:
                    break;
                default:
                    break;

            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
//        if (mediaPlayer.isPlaying()) {
//            int playPositionInMilliseconds = (mediaFileLengthInMilliseconds / 100) * seekBar.getProgress();
//            mediaPlayer.seekTo(playPositionInMilliseconds);
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepareUI();
        checkPlayServicesAvailable();
        populateUI();
    }

    private void populateUI() {
        Realm realm = Realm.getDefaultInstance();
        final RealmResults<Podcast> podcasts = realm.where(Podcast.class).findAll();
        processPodcastsRealm(podcasts);
        podcasts.addChangeListener(new RealmChangeListener<RealmResults<Podcast>>() {
            @Override
            public void onChange(RealmResults<Podcast> podcastRawList) {
                processPodcastsRealm(podcastRawList);
            }
        });
    }

    private void processPodcastsRealm(RealmResults<Podcast> podcastRawList) {
        ArrayList<Podcast> podcastList = new ArrayList<>();
        for (Podcast podcast : podcastRawList) {
            Log.e(TAG, podcast.toString());
            podcastList.add(podcast);
        }
        mAdapter = new PodcastListAdapter(MainActivity.this, podcastList);
        podcastsRecyclerView.setAdapter(mAdapter);
        if (index != -1) {
            mLayoutManager.scrollToPositionWithOffset(index, top);
        }
        if (currentPodcast == null && podcastList.size() > 0 && podcastList.get(0) != null) {
            currentPodcast = podcastList.get(0);
            initDetailsPanel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        index = mLayoutManager.findFirstVisibleItemPosition();
        View v = podcastsRecyclerView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - podcastsRecyclerView.getPaddingTop());
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        IntentFilter syncStateFilter = new IntentFilter();
        syncStateFilter.addAction(SyncTasksService.ACTION_DONE);

        IntentFilter playerStateFilter = new IntentFilter();
        playerStateFilter.addAction(PLAY_ACTION_KEY);
        playerStateFilter.addAction(PAUSE_ACTION_KEY);
        playerStateFilter.addAction(NEXT_ACTION_KEY);
        playerStateFilter.addAction(PREVIOUS_ACTION_KEY);

        manager.registerReceiver(mSyncStatusReceiver, syncStateFilter);
        manager.registerReceiver(mPlayerStatusReceiver, playerStateFilter);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mSyncStatusReceiver);
        manager.unregisterReceiver(mPlayerStatusReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onNoDataUI() {
        //Implement no data ui
    }

    @Override
    public void onSuccessUI() {
        showSimpleToast("Success");
    }

    @Override
    public void onErrorUI() {
        showSimpleToast("Error");
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

        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        int shadowHeight = (int) (getResources().getDimension(R.dimen.bottom_sheet_shadow) / displayMetrics.density);

        final int visiblePartHeight = (int) (UIUtils.getFabSize(this) * displayMetrics.density) +
                (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2) +
                (int) (shadowHeight * displayMetrics.density * 2);

        UIUtils.setViewHeight(visiblePart, visiblePartHeight);

        UIUtils.setViewMargins(detailsWrap, 0, 0, 0,
                (int) (UIUtils.getFabSize(this) * displayMetrics.density) +
                        (int) (UIUtils.getSeekBarHeight(this) * displayMetrics.density * 2) +
                        (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2) +
                        (int) (shadowHeight * displayMetrics.density));

        seekBarProgress = (AppCompatSeekBar) findViewById(R.id.seekBarProgress);
        seekBarProgress.setMax(99);

        podcastsRecyclerView = (RecyclerView) findViewById(R.id.podcast_track_list);
        podcastsRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        podcastsRecyclerView.setLayoutManager(mLayoutManager);


        final int actionBarSize = UIUtils.getActionBarSize(this);
        final int actionBarStabSize = actionBarSize
                - (int) getResources().getDimension(R.dimen.bottom_sheet_shadow)
                - 1;

        actionBarStabView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, actionBarStabSize));


        int fabElevation = (int) (getResources().getDimension(R.dimen.fab_elevation) / displayMetrics.density);

        int fabElevationCompensation = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            UIUtils.setViewMargins(fabPlayPause, 0, 0, 0, 0);
            fabElevationCompensation = (int) (fabElevation * displayMetrics.density * 2);
        }


        behavior.setPeekHeight(
                (int) (UIUtils.getFabSize(this) * displayMetrics.density)
                        + (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2)
                        + actionBarStabSize
                        + (int) (shadowHeight * displayMetrics.density * 2)
                        + fabElevationCompensation);


        UIUtils.setViewMargins(podcastsRecyclerView, 0, 0, 0,
                (int) (UIUtils.getFabSize(this) * displayMetrics.density) +
                        (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2) +
                        (int) (shadowHeight * displayMetrics.density));


        UIUtils.setViewMargins(marqueueTitle, 0, 0,
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

                UIUtils.setViewMargins(marqueueTitle, 0, 0,
                        (int) (((dpWidth * displayMetrics.density) / 3) * slideOffsetNegative), 0);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case StorageUtils.STORAGE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //fire pending request
                    Log.d(TAG, "got permissions");
                } else {
                    //retry
                    Log.d(TAG, "no permissions, need to retry the request");
                }
            }
        }
    }
}
