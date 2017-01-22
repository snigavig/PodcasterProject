package com.goodcodeforfun.podcasterproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.firebase.jobdispatcher.JobService;
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.sync.SyncManager;
import com.goodcodeforfun.podcasterproject.sync.SyncTasksService;
import com.goodcodeforfun.podcasterproject.util.DBUtils;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;
import com.goodcodeforfun.podcasterproject.util.UIUtils;
import com.goodcodeforfun.stateui.StateUIActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_BUFFERING_UPDATE_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_NEXT_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_PLAYBACK_STARTED_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_PLAY_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_PREVIOUS_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_PROGRESS_UPDATE_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.BROADCAST_SUSPEND_ACTION;
import static com.goodcodeforfun.podcasterproject.PlayerService.PAUSED;
import static com.goodcodeforfun.podcasterproject.PlayerService.PLAYING;

public class MainActivity extends StateUIActivity implements AppCompatSeekBar.OnSeekBarChangeListener,
        View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static int index = -1;
    private static int top = -1;
    private FloatingActionButton fabPlayPause;
    private FloatingActionButton fabPrevious;
    private FloatingActionButton fabNext;
    private Animation growAnimation;
    private AppBarLayout mActionBar;
    private AppCompatSeekBar seekBarProgress;
    private RecyclerView podcastsRecyclerView;
    private AppCompatTextView podcastCurrentTime;
    private LinearLayoutManager mLayoutManager;
    private Podcast currentPodcast;
    private PodcastListAdapter mAdapter;
    private AppCompatTextView podcastTime;
    private LinearLayout noDataUI;
    private AppCompatTextView marqueueTitle;
    private DisplayMetrics displayMetrics;
    private float dpScreenWidth;
    private final BottomSheetBehavior.BottomSheetCallback bottomSheetBehavior = new BottomSheetBehavior.BottomSheetCallback() {
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
                default:
                    break;
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            float slideOffsetNegative = (1 - slideOffset);
            float slideOffsetEasingCoefficient = (10 * slideOffsetNegative);
            float slideOffsetEased = slideOffset / slideOffsetEasingCoefficient;
            fabPlayPause.setTranslationX(((dpScreenWidth * displayMetrics.density) / 3) * slideOffsetNegative);
            fabNext.setAlpha(slideOffsetEased);
            fabPrevious.setAlpha(slideOffsetEased);
            seekBarProgress.setAlpha(slideOffsetEased);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActionBar.setElevation(displayMetrics.density * (8 - (slideOffset * 8)));
            }
            podcastsRecyclerView.setAlpha(slideOffsetNegative);

            UIUtils.setViewMargins(marqueueTitle, 0, 0,
                    (int) (((dpScreenWidth * displayMetrics.density) / 3) * slideOffsetNegative), 0);
        }
    };
    private int shadowHeight;
    private int actionBarStabSize;
    private final BroadcastReceiver mSyncStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SyncTasksService.ACTION_SYNC_PODCASTS_DONE)) {
                onStopProgress();
                int result = intent.getIntExtra(SyncTasksService.EXTRA_RESULT, -1);
                if (result != -1 && result == JobService.RESULT_SUCCESS) {
                    populateUI();
                } else {
                    onNoData();
                }
            }
        }
    };
    private final BroadcastReceiver mPlayerStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mediaFileLengthInMilliseconds;
            switch (intent.getAction()) {
                case BROADCAST_PLAY_ACTION:
                    fabPlayPause.setEnabled(true);
                    String primaryKey = intent.getStringExtra(PlayerService.EXTRA_ACTIVE_PODCAST_PRIMARY_KEY_KEY);
                    Realm realm = Realm.getDefaultInstance();
                    currentPodcast = DBUtils.getPodcastByPrimaryKey(realm, primaryKey);
                    initDetailsPanel();
                    break;
                case BROADCAST_PLAYBACK_STARTED_ACTION:
                    mediaFileLengthInMilliseconds = intent.getIntExtra(PlayerService.EXTRA_PODCAST_TOTAL_TIME_KEY, -1);
                    if (mediaFileLengthInMilliseconds != -1) {
                        initPodcastTime(mediaFileLengthInMilliseconds);
                    }
                    setButtonToPlayStateIfPaused();
                    break;
                case BROADCAST_SUSPEND_ACTION:
                    fabPlayPause.setEnabled(true);
                    setButtonToPausedState();
                    break;
                case BROADCAST_NEXT_ACTION:
                    initDetailsPanel();
                    break;
                case BROADCAST_PREVIOUS_ACTION:
                    initDetailsPanel();
                    break;
                case BROADCAST_BUFFERING_UPDATE_ACTION:
                    int bufferingValue = intent.getIntExtra(PlayerService.EXTRA_PODCAST_BUFFERING_VALUE_KEY, -1);
                    if (seekBarProgress != null && bufferingValue != -1) {
                        seekBarProgress.setSecondaryProgress(bufferingValue);
                    }
                    break;
                case BROADCAST_PROGRESS_UPDATE_ACTION:
                    mediaFileLengthInMilliseconds = intent.getIntExtra(PlayerService.EXTRA_PODCAST_TOTAL_TIME_KEY, -1);
                    int currentTime = intent.getIntExtra(PlayerService.EXTRA_PODCAST_CURRENT_TIME_KEY, -1);
                    podcastCurrentTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                            TimeUnit.MILLISECONDS.toHours(currentTime),
                            TimeUnit.MILLISECONDS.toMinutes(currentTime) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentTime)),
                            TimeUnit.MILLISECONDS.toSeconds(currentTime) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentTime))));
                    if (seekBarProgress != null && mediaFileLengthInMilliseconds != -1 && currentTime != -1) {
                        seekBarProgress.setProgress((int) (((float) currentTime / mediaFileLengthInMilliseconds) * 100));
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void setButtonToPausedState() {
        fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow_24dp));
        fabPlayPause.setTag(PAUSED);
    }

    private void setButtonToPlayState() {
        fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pause_24dp));
        fabPlayPause.setTag(PLAYING);
    }

    private void setButtonToPlayStateIfPaused() {
        if (fabPlayPause != null && fabPlayPause.getTag().equals(PAUSED)) {
            setButtonToPlayState();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupLastPodcast();
        prepareUIMetrics();
        prepareUI();
        populateUI();
        if (fabPlayPause.getId() == R.id.play_pause_button &&
                PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastState() !=
                        PlayerService.PAUSED) {
            new MaterialDialog.Builder(this)
                    .title(R.string.restore_confirmation_dialog_title)
                    .content(R.string.restore_confirmation_dialog_content)
                    .positiveText(R.string.restore_confirmation_dialog_positive_text)
                    .negativeText(R.string.restore_confirmation_dialog_negative_text)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            startMediaPlayback(true);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().setLastState(PlayerService.PAUSED);
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    private void setupLastPodcast() {
        String lastPodcast = PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastPodcast();
        if (!"".equals(lastPodcast)) {
            Realm realm = Realm.getDefaultInstance();
            currentPodcast = DBUtils.getPodcastByPrimaryKey(realm, lastPodcast);
        }
    }

    private void prepareUIMetrics() {
        displayMetrics = getResources().getDisplayMetrics();
        dpScreenWidth = displayMetrics.widthPixels / displayMetrics.density;
        shadowHeight = (int) (getResources().getDimension(R.dimen.bottom_sheet_shadow) / displayMetrics.density);

        final int actionBarSize = UIUtils.getActionBarSize(this);
        actionBarStabSize = actionBarSize
                - (int) getResources().getDimension(R.dimen.bottom_sheet_shadow)
                - 1;
    }

    private void populateUI() {
        Realm realm = Realm.getDefaultInstance();
        final RealmResults<Podcast> podcasts = DBUtils.getAllPodcasts(realm);
        if (podcasts.size() > 0) {
            processPodcastsRealm(podcasts);
        } else {
            onProgress();
            SyncManager.startActionSyncPodcastsImmediately(MainActivity.this);
        }
    }

    private void processPodcastsRealm(RealmResults<Podcast> podcastRawList) {
        ArrayList<Podcast> podcastList = new ArrayList<>();
        for (Podcast podcast : podcastRawList) {
            podcastList.add(podcast);
        }
        mAdapter = new PodcastListAdapter(MainActivity.this, podcastList);
        podcastsRecyclerView.setAdapter(mAdapter);
        if (index != -1) {
            mLayoutManager.scrollToPositionWithOffset(index, top);
        }
        if (currentPodcast == null && podcastList.size() > 0 && podcastList.get(0) != null) {
            currentPodcast = podcastList.get(0);
        }
        initDetailsPanel();
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
        syncStateFilter.addAction(SyncTasksService.ACTION_SYNC_PODCASTS_DONE);

        IntentFilter playerStateFilter = new IntentFilter();
        playerStateFilter.addAction(BROADCAST_PLAY_ACTION);
        playerStateFilter.addAction(BROADCAST_PLAYBACK_STARTED_ACTION);
        playerStateFilter.addAction(BROADCAST_SUSPEND_ACTION);
        playerStateFilter.addAction(BROADCAST_NEXT_ACTION);
        playerStateFilter.addAction(BROADCAST_PREVIOUS_ACTION);
        playerStateFilter.addAction(BROADCAST_PROGRESS_UPDATE_ACTION);
        playerStateFilter.addAction(BROADCAST_BUFFERING_UPDATE_ACTION);

        manager.registerReceiver(mSyncStatusReceiver, syncStateFilter);
        manager.registerReceiver(mPlayerStatusReceiver, playerStateFilter);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
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
            case R.id.action_refresh:
                onProgress();
                SyncManager.startActionSyncPodcastsImmediately(MainActivity.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNoDataUI() {
        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            noDataUI.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSuccessUI() {
        noDataUI.setVisibility(View.GONE);
        showSimpleToast("Success");
    }

    @Override
    protected void onErrorUI(String message) {
        showSimpleToast(message);
    }

    private void initSeekBar() {
        if (seekBarProgress == null) {
            seekBarProgress = (AppCompatSeekBar) findViewById(R.id.seekBarProgress);
            seekBarProgress.setMax(99);
            seekBarProgress.setOnSeekBarChangeListener(this);
        }
    }

    private void initPodcastTime(final int millis) {
        if (podcastTime == null) {
            podcastTime = (AppCompatTextView) findViewById(R.id.podcastTotalTimeTextView);
            podcastTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
        }
    }

    private void initDetailsPanel() {
        final FrameLayout detailsWrap = (FrameLayout) findViewById(R.id.detailsWrap);
        final LinearLayout visiblePart = (LinearLayout) detailsWrap.findViewById(R.id.visiblePart);
        final AppCompatImageView podcastBigImageView = (AppCompatImageView) detailsWrap.findViewById(R.id.podcastBigImageView);
        marqueueTitle = (AppCompatTextView) detailsWrap.findViewById(R.id.marqueeTitle);
        podcastCurrentTime = (AppCompatTextView) detailsWrap.findViewById(R.id.podcastCurrentTimeTextView);

        if (marqueueTitle != null && currentPodcast != null) {
            marqueueTitle.setText(currentPodcast.getTitle());

            Glide.with(this)
                    .load(currentPodcast.getImageUrl())
                    .placeholder(
                            BuildConfig.DEFAULT_IS_HIDE_IMAGES
                                    ? R.drawable.header_image
                                    : R.color.colorPrimaryHalfTransparent
                    )
                    .into(podcastBigImageView);
        }
        initSeekBar();
        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_coordinator);
        final View bottomSheet = coordinatorLayout.findViewById(R.id.details_bottom_sheet);

        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);

        final int visiblePartHeight = (int) (UIUtils.getFabSize(this) * displayMetrics.density) +
                (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2) +
                (int) (shadowHeight * displayMetrics.density * 2);

        UIUtils.setViewHeight(visiblePart, visiblePartHeight);

        UIUtils.setViewMargins(detailsWrap, 0, 0, 0,
                (int) (UIUtils.getFabSize(this) * displayMetrics.density) +
                        (int) (UIUtils.getSeekBarHeight(this) * displayMetrics.density * 2) +
                        (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2) +
                        (int) (shadowHeight * displayMetrics.density));

        UIUtils.setViewMargins(marqueueTitle, 0, 0,
                (int) ((dpScreenWidth * displayMetrics.density) / 3), 0);

        //fixes fab elevation problem
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
        behavior.setBottomSheetCallback(bottomSheetBehavior);

        if (currentPodcast != null) {
            Realm realm = Realm.getDefaultInstance();
            Podcast nextPodcast = DBUtils.getNextPodcast(realm, currentPodcast.getOrder());
            if (nextPodcast != null) {
                fabNext.setVisibility(View.VISIBLE);
                fabNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PlayerService.nextMedia(MainActivity.this, currentPodcast.getPrimaryKey());
                    }
                });
            } else {
                fabNext.setVisibility(View.INVISIBLE);
            }

            Podcast previousPodcast = DBUtils.getPreviousPodcast(realm, currentPodcast.getOrder());
            if (previousPodcast != null) {
                fabPrevious.setVisibility(View.VISIBLE);
                fabPrevious.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PlayerService.previousMedia(MainActivity.this, currentPodcast.getPrimaryKey());
                    }
                });
            } else {
                fabPrevious.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        fabPlayPause.setVisibility(View.VISIBLE);
        fabPlayPause.startAnimation(growAnimation);
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.play_pause_button) {
            if (PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastState() == PlayerService.PLAYING) {
                pauseMediaPlayback();
            } else {
                startMediaPlayback(false);
            }
            fabPlayPause.setEnabled(false);
        }
    }

    private void pauseMediaPlayback() {
        if (fabPlayPause != null && currentPodcast != null) {
            PlayerService.pauseMediaPlayback(MainActivity.this);
        }
    }

    private void startMediaPlayback(boolean isRestore /*should restore previous state*/) {
        if (fabPlayPause != null && currentPodcast != null) {
            PlayerService.startMediaPlayback(MainActivity.this, currentPodcast.getPrimaryKey(), isRestore);
        }
    }

    private void prepareUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActionBar = (AppBarLayout) findViewById(R.id.toolbarLayout);
        noDataUI = (LinearLayout) findViewById(R.id.no_data_view);
        FloatingActionButton refreshButton = (FloatingActionButton) findViewById(R.id.refreshImageButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onProgress();
                SyncManager.startActionSyncPodcastsImmediately(MainActivity.this);
            }
        });
        fabPlayPause = (FloatingActionButton) findViewById(R.id.play_pause_button);
        fabPlayPause.setTag(PAUSED);
        fabPrevious = (FloatingActionButton) findViewById(R.id.previous_track_button);
        fabNext = (FloatingActionButton) findViewById(R.id.next_track_button);
        fabPlayPause.setOnClickListener(this);
        fabPrevious.setClickable(false);
        fabNext.setClickable(false);

        final View actionBarStabView = findViewById(R.id.action_bar_stab_view);
        growAnimation = AnimationUtils.loadAnimation(this, R.anim.simple_grow);

        podcastsRecyclerView = (RecyclerView) findViewById(R.id.podcast_track_list);
        podcastsRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        podcastsRecyclerView.setLayoutManager(mLayoutManager);

        actionBarStabView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, actionBarStabSize));

        //pushes from bottom to be able to reach the last item in the list
        UIUtils.setViewMargins(podcastsRecyclerView, 0, 0, 0,
                (int) (UIUtils.getFabSize(this) * displayMetrics.density) +
                        (int) (UIUtils.getFabMargin(this) * displayMetrics.density * 2) +
                        (int) (shadowHeight * displayMetrics.density));

        //moves play/pause button to the right
        fabPlayPause.setTranslationX((dpScreenWidth * displayMetrics.density) / 3);

        initDetailsPanel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == StorageUtils.STORAGE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mAdapter != null) {
                    mAdapter.downloadPodcast(MainActivity.this);
                }
            } else {
                StorageUtils.requestStoragePermissions(MainActivity.this);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        // This is intentionally empty, because we only care about the final destination
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // This is intentionally empty, because we only care about the final destination
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        PlayerService.seekMedia(this, seekBar.getProgress());
    }
}
