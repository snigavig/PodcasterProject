package com.goodcodeforfun.podcasterproject.sync;

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
import com.goodcodeforfun.podcasterproject.model.Podcast;
import com.goodcodeforfun.stateui.StateUIApplication;

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

        Intent intent = new Intent();
        intent.setAction(ACTION_SYNC_PODCASTS_DONE);
        intent.putExtra(EXTRA_TAG, tag);
        intent.putExtra(EXTRA_RESULT, result);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);

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

    private int processRss(OkHttpClient client, String url) {
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
                Log.i(TAG, "Processing feed entry: " + feed.getTitle());
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

                    if (date != null) {
                        podcast.setDate(date);
                    } else {
                        podcast.setDate(new Date());
                    }


                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(podcast);
                        }
                    });

                    Log.i(TAG, "Item title: " + (title == null ? "N/A" : title));
                }
            } catch (XmlPullParserException | IOException | DataFormatException e) {
                e.printStackTrace();
                StateUIApplication.onError();
                return JobService.RESULT_FAIL_RETRY;
            } finally {
                realm.close();
            }

            if (response.code() != 200) {
                StateUIApplication.onError();
                return JobService.RESULT_FAIL_RETRY;
            }
        } catch (IOException e) {
            StateUIApplication.onError();
            Log.e(TAG, "fetchUrl:error" + e.toString());
            return JobService.RESULT_FAIL_RETRY;
        }

        StateUIApplication.onSuccess();
        return JobService.RESULT_SUCCESS;
    }
}