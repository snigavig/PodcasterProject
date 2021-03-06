package com.goodcodeforfun.podcasterproject.sync;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.einmalfel.earl.EarlParser;
import com.einmalfel.earl.Enclosure;
import com.einmalfel.earl.Feed;
import com.einmalfel.earl.Item;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.SimpleJobService;
import com.goodcodeforfun.podcasterproject.BuildConfig;
import com.goodcodeforfun.podcasterproject.PodcasterProjectApplication;
import com.goodcodeforfun.podcasterproject.R;
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.podcasterproject.util.DBUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.DataFormatException;

import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by snigavig on 30.12.16.
 */

public class SyncTasksService extends SimpleJobService {

    public static final String TASK_TAG_INITIAL_SYNC_PODCASTS = "init_sync_podcasts_task";
    public static final String TASK_TAG_SYNC_PODCASTS = "sync_podcasts_task";
    public static final String ACTION_SYNC_PODCASTS_DONE = "SyncTasksService#ACTION_SYNC_PODCASTS_DONE";
    public static final String EXTRA_TAG = "extra_tag";
    public static final String EXTRA_RESULT = "extra_result";
    private static final String TAG = "SyncTasksService";
    private OkHttpClient mClient = new OkHttpClient();

    public static void sendResultBroadcast(Context context, String tag, @JobResult int result) {
        Intent intent = new Intent();
        intent.setAction(ACTION_SYNC_PODCASTS_DONE);
        intent.putExtra(EXTRA_TAG, tag);
        intent.putExtra(EXTRA_RESULT, result);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        manager.sendBroadcast(intent);
    }

    public static int processRss(OkHttpClient client, String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            InputStream inputStream = response.body().byteStream();
            Realm realm = Realm.getDefaultInstance();

            Feed feed;
            //noinspection TryFinallyCanBeTryWithResources
            try {
                feed = EarlParser.parseOrThrow(inputStream, 0);
                int itemOrder = feed.getItems().size();
                for (Item item : feed.getItems()) {
                    final Podcast podcast = new Podcast();
                    String title = item.getTitle();
                    String imageUrl = item.getImageLink();
                    Date date = item.getPublicationDate();
                    String audioUrl = null;
                    Integer audioSize = null;
                    for (Enclosure enclosure : item.getEnclosures()) {
                        if ("audio/mpeg".equals(enclosure.getType())) {
                            audioUrl = enclosure.getLink();
                            audioSize = enclosure.getLength();
                        }
                    }

                    podcast.setTitle(title);
                    podcast.setImageUrl(imageUrl);
                    podcast.setAudioUrl(audioUrl);
                    podcast.setAudioSize(audioSize);
                    podcast.setOrder(itemOrder);
                    itemOrder--;

                    if (date != null) {
                        podcast.setDate(date);
                    } else {
                        podcast.setDate(new Date());
                    }

                    final Podcast oldPodcast = DBUtils.getPodcastByPrimaryKey(realm, audioUrl);

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            if (oldPodcast != null) {
                                podcast.setDownloadProgress(oldPodcast.getDownloadProgress());
                                podcast.setDownloadId(oldPodcast.getDownloadId());
                            }
                            realm.copyToRealmOrUpdate(podcast);
                        }
                    });
                }
            } catch (XmlPullParserException | IOException | DataFormatException e) {
                e.printStackTrace();
                PodcasterProjectApplication.onError(PodcasterProjectApplication.getInstance().getString(R.string.parsing_error_message));
                return retryAction();
            } finally {
                realm.close();
            }

            if (response.code() != 200) {
                PodcasterProjectApplication.onError(PodcasterProjectApplication.getInstance().getString(R.string.network_error_message));
                return retryAction();
            }
        } catch (IOException e) {
            PodcasterProjectApplication.onError(PodcasterProjectApplication.getInstance().getString(R.string.network_error_message));
            Log.e(TAG, "fetchUrl:error" + e.toString());
            return retryAction();
        }

        PodcasterProjectApplication.onSuccess();
        return JobService.RESULT_SUCCESS;
    }

    private static
    @JobResult
    int retryAction() {
        if (SyncManager.isNetworkConnected(PodcasterProjectApplication.getInstance())) {
            return JobService.RESULT_FAIL_RETRY;
        } else {
            return JobService.RESULT_FAIL_NORETRY;
        }
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return super.onStopJob(job);
        //TODO: handle stop job properly
    }

    @Override
    public int onRunJob(JobParameters parameters) {
        String tag = parameters.getTag();
        @JobResult int result = JobService.RESULT_SUCCESS;
        if (TASK_TAG_SYNC_PODCASTS.equals(tag) || TASK_TAG_INITIAL_SYNC_PODCASTS.equals(tag)) {
            result = syncPodcastsTask();
        }

        sendResultBroadcast(this, tag, result);
        return result;
    }

    private
    @JobResult
    int syncPodcastsTask() {
        String url = BuildConfig.PODCAST_URL;
        if (!"".equals(url)) {
            return processRss(mClient, url);
        } else {
            //TODO: implement logic for main client
            return JobService.RESULT_SUCCESS;
        }
    }
}