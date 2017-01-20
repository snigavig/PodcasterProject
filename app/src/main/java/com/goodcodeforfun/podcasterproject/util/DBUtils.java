package com.goodcodeforfun.podcasterproject.util;

import com.goodcodeforfun.podcasterproject.model.Podcast;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by snigavig on 13.01.17.
 */

public class DBUtils {
    public static RealmResults<Podcast> getAllPodcasts(Realm realm) {
        return realm.where(Podcast.class).findAll();
    }

    public static Podcast getPodcastByPrimaryKey(Realm realm, String primaryKey) {
        return realm.where(Podcast.class).equalTo("audioUrl", primaryKey).findFirst();
    }

    public static Podcast getPodcastByDownloadId(Realm realm, long downloadId) {
        return realm.where(Podcast.class).equalTo("downloadId", downloadId).findFirst();
    }

    public static Podcast getNextPodcast(Realm realm, Integer order) {
        return realm.where(Podcast.class).equalTo("order", order - 1).findFirst();
    }

    public static Podcast getPreviousPodcast(Realm realm, Integer order) {
        return realm.where(Podcast.class).equalTo("order", order + 1).findFirst();
    }
}
