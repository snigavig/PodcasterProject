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
}
