package com.goodcodeforfun.podcasterproject;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.util.StorageUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

class PodcastListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final WeakReference<MainActivity> mActivityWeakReference;
    private ArrayList<Podcast> podcastArrayList;

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

        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (!isPositionHeader(position)) {
            PodcastItemViewHolder viewHolder = (PodcastItemViewHolder) holder;
            final Podcast podcast = getItem(position);
            String question = podcast.getTitle();
            viewHolder.tv_title.setText(question);
            final MainActivity activity = mActivityWeakReference.get();
            viewHolder.cv_wrap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS) + "/" + StorageUtils.getFileNameFromUrl(getItem(position).getAudioUrl()));
            if (file.isFile()) {
                viewHolder.downloadButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_archive_red_24dp));
                viewHolder.downloadButton.setEnabled(false);
                viewHolder.downloadButton.setClickable(false);
            } else {
                viewHolder.downloadButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_archive_black_24dp));
                viewHolder.downloadButton.setEnabled(true);
                viewHolder.downloadButton.setClickable(true);
            }

            viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(activity,
                                    Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                                    StorageUtils.STORAGE_PERMISSIONS);
                        }
                    } else {

                        String url = podcast.getAudioUrl();
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setDescription(activity.getString(R.string.download_description));
                        request.setTitle(activity.getString(R.string.download_title));

                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, StorageUtils.getFileNameFromUrl(podcast.getAudioUrl()));

                        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                        manager.enqueue(request);

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

    @Override
    public int getItemCount() {
        return podcastArrayList.size() + 1;
    }

    private class PodcastItemViewHolder extends RecyclerView.ViewHolder {
        final TextView tv_title;
        final ImageView podcastImageView;
        final RelativeLayout cv_wrap;
        final ImageButton downloadButton;

        PodcastItemViewHolder(View view) {
            super(view);
            tv_title = (TextView) view.findViewById(R.id.tv_title);
            podcastImageView = (ImageView) view.findViewById(R.id.podcastImageView);
            downloadButton = (ImageButton) view.findViewById(R.id.downloadButton);
            cv_wrap = (RelativeLayout) view.findViewById(R.id.cv_wrap);
        }
    }

    private class PodcastHeaderViewHolder extends RecyclerView.ViewHolder {
        final ImageView podcastImageView;

        PodcastHeaderViewHolder(View view) {
            super(view);
            podcastImageView = (ImageView) view.findViewById(R.id.coverImageView);
        }
    }
}
