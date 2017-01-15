package com.goodcodeforfun.podcasterproject;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
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
import com.f2prateek.progressbutton.ProgressButton;
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.util.DBUtils;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.realm.Realm;

class PodcastListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = PodcastListAdapter.class.getSimpleName();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final WeakReference<MainActivity> mActivityWeakReference;
    private final Handler handler = new Handler();
    private ArrayList<Podcast> podcastArrayList;
    private LongSparseArray<ProgressButton> downloadingProgressButtons = new LongSparseArray<>();
    private LongSparseArray<Runnable> downloadingProgressRunnables = new LongSparseArray<>();
    private String downloadPodcastUrl;
    private ProgressButton progressButton;
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
            if (downloadingProgressRunnables.get(viewHolder.getDownloadId()) != null) {
                downloadingProgressRunnables.remove(viewHolder.getDownloadId());
            }
            if (downloadingProgressButtons.get(viewHolder.getDownloadId()) != null) {
                downloadingProgressButtons.remove(viewHolder.getDownloadId());
            }
            viewHolder.setDownloadId(null);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (!isPositionHeader(position)) {
            PodcastItemViewHolder viewHolder = (PodcastItemViewHolder) holder;
            final Podcast podcast = getItem(position);
            viewHolder.setDownloadId(podcast.getDownloadId());
            String title = podcast.getTitle();
            viewHolder.tv_title.setText(title);
            final MainActivity activity = mActivityWeakReference.get();
            viewHolder.cv_wrap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PlayerService.stopMediaPlayback(activity);
                    PlayerService.startMediaPlayback(activity, podcast.getPrimaryKey(), false /*should NOT restore previous state*/);
                }
            });

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS) + "/" + StorageUtils.getFileNameFromUrl(getItem(position).getAudioUrl()));
            if (podcast.isDownloadInited()) {
                viewHolder.downloadButton.setPinned(true);
                int progress = podcast.getDownloadProgress();
                if (progress != -1) {
                    viewHolder.downloadButton.setProgress(progress);
                    downloadingProgressRunnables.remove(podcast.getDownloadId());
                    Runnable updateDownloadProgress = new Runnable() {
                        public void run() {
                            downloadProgressUpdater(podcast.getDownloadId(), false);
                        }
                    };
                    downloadingProgressRunnables.append(podcast.getDownloadId(), updateDownloadProgress);
                    downloadingProgressButtons.append(podcast.getDownloadId(), viewHolder.downloadButton);
                }
                //viewHolder.downloadButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_archive_red_24dp));
                viewHolder.downloadButton.setEnabled(false);
                viewHolder.downloadButton.setClickable(false);
            } else {
                viewHolder.downloadButton.setProgress(0);
                viewHolder.downloadButton.setPinned(false);
                //viewHolder.downloadButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_archive_black_24dp));
                viewHolder.downloadButton.setEnabled(true);
                viewHolder.downloadButton.setClickable(true);
            }

            viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    downloadPodcastUrl = podcast.getAudioUrl();
                    progressButton = (ProgressButton) view;
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

    private void downloadProgressUpdater(final long downloadId, boolean isInitStart) {
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(downloadId);

        Cursor cursor = manager.query(q);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                //FINISHED DOWNLOAD
                return;
            }

            final int dl_progress = (int) ((bytes_downloaded * 100L) / bytes_total);
            Log.e("PROGRESS: ", String.valueOf(dl_progress));
            if (downloadingProgressButtons.get(downloadId) != null) {
                downloadingProgressButtons.get(downloadId).setProgress(dl_progress);
            }
        }
        cursor.close();
        Runnable updateDownloadProgress = new Runnable() {
            public void run() {
                downloadProgressUpdater(downloadId, false);
            }
        };

        Log.e("INIT::: ", String.valueOf(isInitStart));
        Log.e("RNBL::: ", downloadingProgressRunnables.get(downloadId) != null ? downloadingProgressRunnables.get(downloadId).toString() : "NULL");

        if (!isInitStart && downloadingProgressRunnables.get(downloadId) != null) {
            downloadingProgressRunnables.remove(downloadId);
        }
        if (downloadingProgressButtons.get(downloadId) != null) {
            downloadingProgressRunnables.append(downloadId, updateDownloadProgress);
            handler.postDelayed(updateDownloadProgress, 1000);
        }
    }

    public void downloadPodcast(MainActivity activity) {
        if (manager == null) {
            manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        }
        if (downloadPodcastUrl != null && progressButton != null) {
            final long downloadId = StorageUtils.downloadFile(activity, downloadPodcastUrl);
            Realm realm = Realm.getDefaultInstance();
            final Podcast currentDownloadedPodcast = DBUtils.getPodcastByPrimaryKey(realm, downloadPodcastUrl);

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    currentDownloadedPodcast.setDownloadId(downloadId);
                    realm.copyToRealmOrUpdate(currentDownloadedPodcast);
                }
            });
            downloadingProgressButtons.append(downloadId, progressButton);
            downloadProgressUpdater(downloadId, true);
        }
    }

    @Override
    public int getItemCount() {
        return podcastArrayList.size() + 1;
    }

    private class PodcastItemViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView tv_title;
        private final AppCompatImageView podcastImageView;
        private final RelativeLayout cv_wrap;
        private final ProgressButton downloadButton;
        private Long downloadId;

        PodcastItemViewHolder(View view) {
            super(view);
            tv_title = (AppCompatTextView) view.findViewById(R.id.tv_title);
            podcastImageView = (AppCompatImageView) view.findViewById(R.id.podcastImageView);
            downloadButton = (ProgressButton) view.findViewById(R.id.downloadButton);
            cv_wrap = (RelativeLayout) view.findViewById(R.id.cv_wrap);
            downloadId = 0L;
        }

        public Long getDownloadId() {
            return downloadId;
        }

        public void setDownloadId(Long downloadId) {
            this.downloadId = downloadId;
        }
    }

    private class PodcastHeaderViewHolder extends RecyclerView.ViewHolder {

        PodcastHeaderViewHolder(View view) {
            super(view);
        }
    }
}
