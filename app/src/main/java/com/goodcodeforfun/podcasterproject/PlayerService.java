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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.util.Foreground;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.realm.Realm;

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
    public static final String START_PLAY_ACTION = "PlayerService#ACTION_START";
    public static final String STOP_PLAY_ACTION = "PlayerService#ACTION_STOP";
    public static final String SEEK_ACTION_KEY = "PlayerService#ACTION_SEEK";
    public static final String BROADCAST_PREVIOUS_ACTION_KEY = "PlayerService#ACTION_PREVIOUS";
    public static final String BROADCAST_PAUSE_ACTION_KEY = "PlayerService#ACTION_PAUSE";
    public static final String BROADCAST_PLAY_ACTION_KEY = "PlayerService#ACTION_PLAY";
    public static final String BROADCAST_NEXT_ACTION_KEY = "PlayerService#ACTION_NEXT";
    public static final String BROADCAST_UPDATE_ACTION_KEY = "PlayerService#ACTION_UPDATE";
    public static final String BROADCAST_BUFFERING_UPDATE_ACTION_KEY = "PlayerService#ACTION_BUFFERING_UPDATE";
    public static final String EXTRA_PODCAST_TOTAL_TIME_KEY = "EXTRA_PODCAST_TOTAL_TIME";
    public static final String EXTRA_PODCAST_CURRENT_TIME_KEY = "EXTRA_PODCAST_CURRENT_TIME";
    public static final String EXTRA_PODCAST_PRIMARY_KEY_KEY = "EXTRA_PODCAST_PRIMARY_KEY";
    public static final String EXTRA_PODCAST_BUFFERING_VALUE_KEY = "EXTRA_PODCAST_BUFFERING_VALUE";
    public static final String EXTRA_PODCAST_SEEK_PROGRESS_VALUE_KEY = "EXTRA_PODCAST_SEEK_PROGRESS_VALUE";
    private static final String TAG = PlayerService.class.getSimpleName();
    private static final String LOCK_TAG = TAG + ".lock";
    private final Handler handler = new Handler();
    private final Foreground.Listener myListener = new Foreground.Listener() {
        public void onBecameForeground() {
            //initSeekBar();
        }

        public void onBecameBackground() {
            //seekBarProgress = null;
        }
    };
    private int mediaFileLengthInMilliseconds = -1;
    private PowerManager.WakeLock mWakeLock;
    private MediaPlayer mediaPlayer;
    private boolean isPaused = true;
    private String activePodcastName;

    public PlayerService() {
    }

    public static void startMediaPlayback(Context context, String primaryKey) {
        PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().setLastPodcast(primaryKey);
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.putExtra(EXTRA_PODCAST_PRIMARY_KEY_KEY, primaryKey);
        podcastPlayerServiceIntent.setAction(START_PLAY_ACTION);
        context.startService(podcastPlayerServiceIntent);
    }

    public static void seekMedia(Context context, int progress) {
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.putExtra(EXTRA_PODCAST_SEEK_PROGRESS_VALUE_KEY, progress);
        podcastPlayerServiceIntent.setAction(SEEK_ACTION_KEY);
        context.startService(podcastPlayerServiceIntent);
    }

    public static void stopMediaPlayback(Context context) {
        Intent podcastPlayerServiceIntent = new Intent(context, PlayerService.class);
        podcastPlayerServiceIntent.setAction(STOP_PLAY_ACTION);
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
            stopForegroundPlayerService(PodcasterProjectApplication.getInstance());
            mediaPlayer = null;
        }
    }

    private void prepareMediaPlayer(String audioUrl) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        try {
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS) + "/" + StorageUtils.getFileNameFromUrl(audioUrl);
            File file = new File(filePath);
            String dataSource;
            if (file.isFile()) {
                dataSource = filePath;
            } else {
                dataSource = audioUrl;
            }
            mediaPlayer.setDataSource(dataSource);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
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
    }

    private void handleIntent(Intent intent) {
        if (START_FOREGROUND_ACTION.equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID.FOREGROUND_SERVICE, buildNotification());
        } else if (UPDATE_FOREGROUND_ACTION.equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID.FOREGROUND_SERVICE, buildNotification());
        } else if (SEEK_ACTION_KEY.equals(intent.getAction())) {
            int progress = intent.getIntExtra(EXTRA_PODCAST_SEEK_PROGRESS_VALUE_KEY, -1);
            Log.e("Got progress", String.valueOf(progress));
            if (mediaPlayer.isPlaying() && progress != -1) {
                int playPositionInMilliseconds = (mediaFileLengthInMilliseconds / 100) * progress;
                mediaPlayer.seekTo(playPositionInMilliseconds);
            }
        } else if (START_PLAY_ACTION.equals(intent.getAction())) {
            String primaryKey = intent.getStringExtra(EXTRA_PODCAST_PRIMARY_KEY_KEY);
            Realm realm = Realm.getDefaultInstance();
            Podcast podcast = realm.where(Podcast.class).equalTo("audioUrl", primaryKey).findFirst();
            activePodcastName = podcast.getTitle();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG);
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            if (null != mediaPlayer) {
                if (isPaused) {
                    isPaused = false;
                    mediaPlayer.start();
                    startForegroundPlayerService(PodcasterProjectApplication.getInstance());
                    primaryProgressUpdater();
                } else {
                    if (!mediaPlayer.isPlaying()) {
                        clearMediaPlayer();
                        prepareMediaPlayer(podcast.getAudioUrl());
                    }
                }
            } else {
                prepareMediaPlayer(podcast.getAudioUrl());
            }
        } else if (STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
            stopForeground(true);
        } else if (STOP_PLAY_ACTION.equals(intent.getAction())) {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                isPaused = true;
            }
            //stopSelf();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            Thread t = new Thread() {
                public void run() {
                    Log.e("Got intent: ", intent.getAction());
                    handleIntent(intent);
                }
            };
            t.start();
        }
        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
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

        final String lastPodcastPrimaryKey = PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastPodcast();

//        final Cursor cursorPrevious = PodcastsDBHelper.getPodcastsCursorPreviousById(this, lastPodcastId);
//
//        if (cursorPrevious.getCount() > 0) {
//            Intent previousIntent = new Intent();
//            previousIntent.setAction(ACTION.BROADCAST_PREVIOUS_ACTION_KEY);
//            PendingIntent pendingIntentPrevious = PendingIntent.getBroadcast(this, 12345, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            builder.addAction(R.drawable.ic_skip_previous_24dp, "", pendingIntentPrevious);
//        }
//        cursorPrevious.close();

        Intent playPauseIntent = new Intent();

        if (PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastState() == PLAYING) {
            playPauseIntent.setAction(BROADCAST_PAUSE_ACTION_KEY);
            PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 12345, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_pause_24dp, "", pendingIntentPlayPause);
        } else if (PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().getLastState() == STOPPED) {
            //TODO: implement stopped state
        } else {
            playPauseIntent.setAction(BROADCAST_PLAY_ACTION_KEY);
            PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 12345, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_play_arrow_24dp, "", pendingIntentPlayPause);
        }

//        final Cursor cursorNext = PodcastsDBHelper.getPodcastsCursorNextById(this, lastPodcastId);
//
//        if (cursorNext.getCount() > 0) {
//            Intent nextIntent = new Intent();
//            nextIntent.setAction(ACTION.BROADCAST_NEXT_ACTION_KEY);
//            PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 12345, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            builder.addAction(R.drawable.ic_skip_next_24dp, "", pendingIntentNext);
//        }
//        cursorNext.close();

        return builder.build();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Log.e("BUFFERING UPDATE", String.valueOf(i));
        sendBufferingUpdateBroadcast(i);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        //TODO: implement completion handler
    }

    private void sendBufferingUpdateBroadcast(int bufferingValue) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_BUFFERING_UPDATE_ACTION_KEY);
        intent.putExtra(EXTRA_PODCAST_BUFFERING_VALUE_KEY, bufferingValue);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    private void sendUpdateBroadcast() {
        PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().setLastPodcastTime(mediaPlayer.getCurrentPosition());
        Intent intent = new Intent();
        intent.setAction(BROADCAST_UPDATE_ACTION_KEY);
        intent.putExtra(EXTRA_PODCAST_CURRENT_TIME_KEY, mediaPlayer.getCurrentPosition());
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    private void primaryProgressUpdater() {
        if (mediaPlayer.isPlaying()) {
            sendUpdateBroadcast();
            Runnable notification = new Runnable() {
                public void run() {
                    primaryProgressUpdater();
                }
            };
            handler.postDelayed(notification, 1000);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaFileLengthInMilliseconds = mediaPlayer.getDuration();

        Intent intent = new Intent();
        intent.setAction(BROADCAST_PLAY_ACTION_KEY);
        intent.putExtra(EXTRA_PODCAST_TOTAL_TIME_KEY, mediaFileLengthInMilliseconds);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);

        mediaPlayer.start();
        startForegroundPlayerService(PodcasterProjectApplication.getInstance());
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