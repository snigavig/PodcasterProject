package com.goodcodeforfun.podcasterproject.util;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import com.goodcodeforfun.podcasterproject.MainActivity;
import com.goodcodeforfun.podcasterproject.R;

/**
 * Created by snigavig on 01.01.17.
 */

public class StorageUtils {
    public static final int STORAGE_PERMISSIONS = 12345;

    public static String getFileNameFromUrl(String url) {
        String result;
        String[] separated = url.split("/");
        result = separated[separated.length - 1];
        return result;
    }

    public static void requestStoragePermissions(MainActivity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                StorageUtils.STORAGE_PERMISSIONS);
    }

    public static long downloadFile(Context context, String downloadPodcastUrl, String title) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadPodcastUrl));
        request.setDescription(context.getString(R.string.download_description));
        request.setTitle(title);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, StorageUtils.getFileNameFromUrl(downloadPodcastUrl));
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
    }

    private static int getDownloadFileStatus(Cursor c) {
        return c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
    }
}
