package org.mozilla.cachedrequestloader;

import android.content.Context;
import android.net.TrafficStats;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.httprequest.HttpRequest;
import org.mozilla.threadutils.ThreadUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundCachedRequestLoader {

    private static final ExecutorService backgroundExecutorService = Executors.newFixedThreadPool(2);

    private Context context;
    private String subscriptionKey;
    private String subscriptionUrl;
    private String userAgent;
    private int socketTag;
    private ResponseData stringLiveData;
    private static final String TAG = "CachedRequestLoader";

    public BackgroundCachedRequestLoader(Context context, String subscriptionKey, String subscriptionUrl, String userAgent, int socketTag) {
        this.context = context;
        this.subscriptionKey = subscriptionKey;
        this.subscriptionUrl = subscriptionUrl;
        this.userAgent = userAgent;
        this.socketTag = socketTag;
    }

    public ResponseData getStringLiveData() {
        if (stringLiveData == null) {
            stringLiveData = new ResponseData();
            loadFromCache();
            loadFromRemote();
        }
        return stringLiveData;
    }

    private void loadFromCache() {
        backgroundExecutorService.submit(() -> {
            try {
                String string = FileUtils.readStringFromFile(new FileUtils.GetCache(new WeakReference<>(context)).get(), subscriptionKey);
                stringLiveData.postValue(new Pair<>(ResponseData.SOURCE_CACHE, string));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to open Cache directory when reading cached banner config");
            }
        });
    }

    private void loadFromRemote() {
        backgroundExecutorService.submit(() -> {
            TrafficStats.setThreadStatsTag(socketTag);
            try {
                String string = HttpRequest.get(new URL(subscriptionUrl), userAgent);
                string = string.replace("\n", "");
                stringLiveData.postValue(new Pair<>(ResponseData.SOURCE_NETWORK, string));
                if (TextUtils.isEmpty(string)) {
                    deleteCache();
                } else {
                    writeToCache(string);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
    }

    private void writeToCache(String string) {
        try {
            final Runnable runnable = new FileUtils.WriteStringToFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), subscriptionKey), string);
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to open cache directory when writing to cache.");
        }
    }

    private void deleteCache() {
        try {
            final Runnable runnable = new FileUtils.DeleteFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), subscriptionKey));
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to open cache directory when deleting cache.");
        }
    }
}
