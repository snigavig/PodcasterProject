package com.goodcodeforfun.podcasterproject.util;

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
}
