package com.goodcodeforfun.podcasterproject;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.dd.CircularProgressButton;
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.util.DBUtils;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

class PodcastListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = PodcastListAdapter.class.getSimpleName();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final WeakReference<MainActivity> mActivityWeakReference;
    private ArrayList<Podcast> podcastArrayList;
    private LongSparseArray<CircularProgressButton> downloadingProgressButtons = new LongSparseArray<>();
    private LongSparseArray<Timer> downloadingProgressTimers = new LongSparseArray<>();
    private String downloadPodcastUrl;
    private CircularProgressButton currentProgressButton;
    private DownloadManager manager = null;

    PodcastListAdapter(MainActivity activity, ArrayList<Podcast> podcastArrayList) {
        this.podcastArrayList = podcastArrayList;
        this.mActivityWeakReference = new WeakReference<>(activity);
    }

    private Podcast getItem(int position) {
        return podcastArrayList.get(position - 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.podcast_list_item_layout, parent, false);
            return new PodcastItemViewHolder(view);
        } else if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.podcast_list_header, parent, false);
            return new PodcastHeaderViewHolder(view);
        }
        Log.e(TAG, "there is no type that matches the type " + viewType + " + make sure your using types correctly");
        return null;
    }


    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof PodcastItemViewHolder) {
            PodcastItemViewHolder viewHolder = (PodcastItemViewHolder) holder;
            downloadingProgressButtons.remove(Long.valueOf(viewHolder.downloadButton.getTag().toString()));
            viewHolder.downloadButton.setTag(null);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (!isPositionHeader(position)) {
            PodcastItemViewHolder viewHolder = (PodcastItemViewHolder) holder;
            final Podcast podcast = getItem(position);
            viewHolder.downloadButton.setTag(podcast.getDownloadId());
            String title = podcast.getTitle();
            viewHolder.podcastTitleTextView.setText(title);
            final MainActivity activity = mActivityWeakReference.get();
            viewHolder.cardWrap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PlayerService.stopMediaPlayback(activity);
                    PlayerService.startMediaPlayback(activity, podcast.getPrimaryKey(), false /*should NOT restore previous state*/);
                }
            });

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS) + "/" + StorageUtils.getFileNameFromUrl(getItem(position).getAudioUrl()));
            if (podcast.isDownloaded() || podcast.isDownloadInitiated() || file.isFile()) {
                //viewHolder.downloadButton.setPinned(true);
            } else {
                //viewHolder.downloadButton.setPinned(false);
            }
            viewHolder.downloadButton.setProgress(0);
            if (podcast.isDownloadInitiated() && !podcast.isDownloaded()) {
                int progress = podcast.getDownloadProgress();
                if (progress != -1) {
                    viewHolder.downloadButton.setProgress(progress);
                    downloadingProgressButtons.append(podcast.getDownloadId(), viewHolder.downloadButton);
                    startDownloadTimerTask(podcast.getDownloadId());
                }
                viewHolder.downloadButton.setEnabled(false);
                viewHolder.downloadButton.setClickable(false);
            } else {
                viewHolder.downloadButton.setProgress(0);
                viewHolder.downloadButton.setEnabled(true);
                viewHolder.downloadButton.setClickable(true);
            }

            viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    downloadPodcastUrl = podcast.getAudioUrl();
                    currentProgressButton = (CircularProgressButton) view;
                    if (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(activity,
                                    Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            StorageUtils.requestStoragePermissions(activity);
                        }
                    } else {
                        downloadPodcast(activity);
                    }
                }
            });

            if (!PodcasterProjectApplication.getInstance().getSharedPreferencesUtils().isHideImages()) {
                viewHolder.podcastImageView.setVisibility(View.VISIBLE);
                Glide.with(activity)
                        .load(podcast.getImageUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .into(viewHolder.podcastImageView);
            } else {
                viewHolder.podcastImageView.setVisibility(View.GONE);
            }
        }

    }

    private void updateDownloadProgress(final long downloadId) {
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(downloadId);
        final MainActivity activity = mActivityWeakReference.get();

        if (manager == null) {
            manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        }
        Cursor cursor = manager.query(q);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                //download finished
                stopDownloadTimerTask(downloadId);
                return;
            }

            final int downloadProgressValue = (int) ((bytes_downloaded * 100L) / bytes_total);

            Realm realm = Realm.getDefaultInstance();
            final Podcast podcast = DBUtils.getPodcastByDownloadId(realm, downloadId);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    podcast.setDownloadProgress(downloadProgressValue);
                    realm.copyToRealmOrUpdate(podcast);
                }
            });
            realm.close();
            setDownloadProgress(activity, downloadId, downloadProgressValue);
        }
        cursor.close();
    }

    private void setDownloadProgress(MainActivity activity, final long downloadId, final int downloadProgressValue) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (downloadingProgressButtons.get(downloadId) != null) {
                        downloadingProgressButtons.get(downloadId).setProgress(downloadProgressValue);
                    }
                }
            });
        }
    }

    public void downloadPodcast(MainActivity activity) {
        if (manager == null) {
            manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        }
        if (downloadPodcastUrl != null && currentProgressButton != null) {
            Realm realm = Realm.getDefaultInstance();
            final Podcast currentDownloadedPodcast = DBUtils.getPodcastByPrimaryKey(realm, downloadPodcastUrl);
            final long downloadId = StorageUtils.downloadFile(activity, downloadPodcastUrl, currentDownloadedPodcast.getTitle());
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    currentDownloadedPodcast.setDownloadId(downloadId);
                    realm.copyToRealmOrUpdate(currentDownloadedPodcast);
                }
            });
            currentProgressButton.setTag(downloadId);
            downloadingProgressButtons.append(downloadId, currentProgressButton);
            startDownloadTimerTask(downloadId);
            realm.close();
        }
    }

    private void startDownloadTimerTask(final long downloadId) {
        Timer downloadProgressTimer = new Timer();
        int delay = 1000;
        downloadProgressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateDownloadProgress(downloadId);
            }
        }, 0, delay);
        downloadingProgressTimers.append(downloadId, downloadProgressTimer);
    }

    private void stopDownloadTimerTask(long downloadId) {
        Timer downloadProgressTimer = downloadingProgressTimers.get(downloadId);
        final MainActivity activity = mActivityWeakReference.get();
        setDownloadProgress(activity, downloadId, 0);
        Realm realm = Realm.getDefaultInstance();
        final Podcast podcast = DBUtils.getPodcastByDownloadId(realm, downloadId);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                podcast.setDownloadProgress(100);
                realm.copyToRealmOrUpdate(podcast);
            }
        });
        realm.close();
        downloadingProgressButtons.remove(downloadId);
        if (downloadProgressTimer != null) {
            downloadProgressTimer.cancel();
            downloadProgressTimer.purge();
            downloadingProgressTimers.remove(downloadId);
        }
    }

    @Override
    public int getItemCount() {
        return podcastArrayList.size() + 1;
    }

    private class PodcastItemViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView podcastTitleTextView;
        private final AppCompatImageView podcastImageView;
        private final RelativeLayout cardWrap;
        private final CircularProgressButton downloadButton;

        PodcastItemViewHolder(View view) {
            super(view);
            podcastTitleTextView = (AppCompatTextView) view.findViewById(R.id.tv_title);
            podcastImageView = (AppCompatImageView) view.findViewById(R.id.podcastImageView);
            downloadButton = (CircularProgressButton) view.findViewById(R.id.downloadButton);
            cardWrap = (RelativeLayout) view.findViewById(R.id.cv_wrap);
        }
    }

    private class PodcastHeaderViewHolder extends RecyclerView.ViewHolder {

        PodcastHeaderViewHolder(View view) {
            super(view);
        }
    }
}
