package com.goodcodeforfun.podcasterproject.model;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by snigavig on 31.12.16.
 */

public class Podcast extends RealmObject {
    @Required
    private String title;
    private String imageUrl;
    @Required
    @PrimaryKey
    private String audioUrl;
    private Integer audioSize;
    private String audioFile;
    @Required
    private Date date;
    private Integer downloadProgress = -1;
    private Long downloadId = 0L;
    private Integer order;

    public String getPrimaryKey() {
        return audioUrl;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(String audioFile) {
        this.audioFile = audioFile;
    }

    public Integer getAudioSize() {
        return audioSize;
    }

    public void setAudioSize(Integer audioSize) {
        this.audioSize = audioSize;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean isDownloaded() {
        return downloadProgress == 100;
    }

    public Boolean isDownloadInitiated() {
        return downloadProgress != -1;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int progress) {
        downloadProgress = progress;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(Long downloadId) {
        this.downloadId = downloadId;
    }
}