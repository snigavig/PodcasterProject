package com.goodcodeforfun.podcasterproject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;

import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.util.Foreground;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;
import com.goodcodeforfun.stateui.StateUIApplication;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PlayerService extends Service implements
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    public static final int PLAYING = 0;
    public static final int PAUSED = 1;
    public static final int STOPPED = 2;
    public static final String MAIN_ACTION = "PlayerService#ACTION_MAIN";
    public static final String UPDATE_FOREGROUND_ACTION = "PlayerService#.ACTION_UPDATE_FOREGROUND";
    public static final String START_FOREGROUND_ACTION = "PlayerService#ACTION_START_FOREGROUND";
    public static final String STOP_FOREGROUND_ACTION = "PlayerService#ACTION_STOP_FOREGROUND";
    public static final String START_ACTION = "PlayerService#ACTION_START";
    public static final String STOP_ACTION = "PlayerService#ACTION_STOP";
    public static final String PREVIOUS_ACTION_KEY = "PlayerService#ACTION_PREVIOUS";
    public static final String PAUSE_ACTION_KEY = "PlayerService#ACTION_PAUSE";
    public static final String PLAY_ACTION_KEY = "PlayerService#ACTION_PLAY";
    public static final String NEXT_ACTION_KEY = "PlayerService#ACTION_NEXT";
    public static final String SEEK_ACTION_KEY = "PlayerService#ACTION_SEEK";
    private static final String TAG = PlayerService.class.getSimpleName();
    private static final String LOCK_TAG = TAG + ".lock";
    private final Handler handler = new Handler();

    //    private void initSeekBar() {
//        if (seekBarProgress == null) {
//            if (mActivityWeakReference != null) {
//                MainActivity activity = mActivityWeakReference.get();
//                seekBarProgress = (SeekBar) activity.findViewById(R.id.seekBarProgress);
//                seekBarProgress.setOnSeekBarChangeListener(PodcastPlayerService.this);
//            }
//        }
//    }
    private final Foreground.Listener myListener = new Foreground.Listener() {
        public void onBecameForeground() {
            //initSeekBar();
        }

        public void onBecameBackground() {
            //seekBarProgress = null;
        }
    };
    private PowerManager.WakeLock mWakeLock;

    //    private void initPodcastTime(final int millis) {
//        if (podcastTime == null) {
//            if (mActivityWeakReference != null) {
//                MainActivity activity = mActivityWeakReference.get();
//                podcastTime = (TextView) activity.findViewById(R.id.podcastLengthTextView);
//                activity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        podcastTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
//                                TimeUnit.MILLISECONDS.toHours(millis),
//                                TimeUnit.MILLISECONDS.toMinutes(millis) -
//                                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
//                                TimeUnit.MILLISECONDS.toSeconds(millis) -
//                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
//                    }
//                });
//            }
//        }
//    }
    private MediaPlayer mediaPlayer;
    private boolean isPaused = true;
    private String activePodcastName;
    private int mediaFileLengthInMilliseconds;

    public PlayerService() {
    }

    public static void initPlayerService(Context context, Integer id) {
        PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().setLastPodcast(id);
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.setAction(START_ACTION);
        context.startService(podcastPlayerServiceIntent);
    }

    private static void startForegroundPlayerService(Context context) {
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.setAction(START_FOREGROUND_ACTION);
        context.startService(podcastPlayerServiceIntent);
    }

    private static void stopForegroundPlayerService(Context context) {
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.setAction(STOP_FOREGROUND_ACTION);
        context.startService(podcastPlayerServiceIntent);
    }

    public static void updateForegroundPlayerService(Context context) {
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.setAction(UPDATE_FOREGROUND_ACTION);
        context.startService(podcastPlayerServiceIntent);
    }

    private void clearMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            //stopForegroundPlayerService(mActivityWeakReference.get());
            mediaPlayer = null;
        }
    }

    private void prepareMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        //PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastPodcast();

        try {
            Podcast podcast;
            //noinspection TryFinallyCanBeTryWithResources
            //try {
            //    while (cursor.moveToNext()) {
            podcast = new Podcast();
            activePodcastName = podcast.getTitle();
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS) + "/" + StorageUtils.getFileNameFromUrl(podcast.getAudioUrl());
            File file = new File(filePath);
            String dataSource;
            if (file.isFile()) {
                dataSource = filePath;
            } else {
                dataSource = podcast.getAudioUrl();
            }
            mediaPlayer.setDataSource(dataSource);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
            //}
//            } finally {
//                cursor.close();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Foreground.get(this).addListener(myListener);
        //initSeekBar();
    }

    private void handleIntent(Intent intent) {
        if (START_FOREGROUND_ACTION.equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID.FOREGROUND_SERVICE, buildNotification());
        } else if (UPDATE_FOREGROUND_ACTION.equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID.FOREGROUND_SERVICE, buildNotification());
        } else if (START_ACTION.equals(intent.getAction())) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG);
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        } else if (STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
            stopForeground(true);
        } else if (STOP_ACTION.equals(intent.getAction())) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            Thread t = new Thread() {
                public void run() {
                    handleIntent(intent);
                }
            };
            t.start();
        }
        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
        Foreground.get(this).removeListener(myListener);
    }

    private Notification buildNotification() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(activePodcastName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent);

        builder.setPriority(Notification.PRIORITY_MAX);

        final int lastPodcastId = PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastPodcast();

//        final Cursor cursorPrevious = PodcastsDBHelper.getPodcastsCursorPreviousById(this, lastPodcastId);
//
//        if (cursorPrevious.getCount() > 0) {
//            Intent previousIntent = new Intent();
//            previousIntent.setAction(ACTION.PREVIOUS_ACTION_KEY);
//            PendingIntent pendingIntentPrevious = PendingIntent.getBroadcast(this, 12345, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            builder.addAction(R.drawable.ic_skip_previous_24dp, "", pendingIntentPrevious);
//        }
//        cursorPrevious.close();

        Intent playPauseIntent = new Intent();

        if (PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastState() == PLAYING) {
            playPauseIntent.setAction(PAUSE_ACTION_KEY);
            PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 12345, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_pause_24dp, "", pendingIntentPlayPause);
        } else if (PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastState() == STOPPED) {
            //TODO: implement stopped state
        } else {
            playPauseIntent.setAction(PLAY_ACTION_KEY);
            PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 12345, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_play_arrow_24dp, "", pendingIntentPlayPause);
        }

//        final Cursor cursorNext = PodcastsDBHelper.getPodcastsCursorNextById(this, lastPodcastId);
//
//        if (cursorNext.getCount() > 0) {
//            Intent nextIntent = new Intent();
//            nextIntent.setAction(ACTION.NEXT_ACTION_KEY);
//            PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 12345, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            builder.addAction(R.drawable.ic_skip_next_24dp, "", pendingIntentNext);
//        }
//        cursorNext.close();

        return builder.build();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
//        if (seekBarProgress != null) {
//            seekBarProgress.setSecondaryProgress(i);
//        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {


    }

    private void primaryProgressUpdater() {
//        if (seekBarProgress != null) {
//            seekBarProgress.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLengthInMilliseconds) * 100));
//        }
//        PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().setLastPodcastTime(mediaPlayer.getCurrentPosition());
//        if (mediaPlayer.isPlaying()) {
//            Runnable notification = new Runnable() {
//                public void run() {
//                    primaryProgressUpdater();
//                }
//            };
//            handler.postDelayed(notification, 1000);
//        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
        //initPodcastTime(mediaFileLengthInMilliseconds);
        mediaPlayer.start();
        startForegroundPlayerService(StateUIApplication.getContext());
        isPaused = false;
        PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().setLastState(PLAYING);
        primaryProgressUpdater();
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PLAYING, PAUSED})
    public @interface PlayerState {
    }
}