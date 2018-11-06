package org.mozilla.cachedrequestloader;

import android.content.Context;
import android.net.TrafficStats;
import android.support.annotation.VisibleForTesting;
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

public class BackgroundCachedRequestLoader implements RequestLoaderDelegation.RequestLoader {

    private static final ExecutorService backgroundExecutorService = Executors.newFixedThreadPool(2);

    private RequestLoaderDelegation requestLoaderDelegation;
    private static final String TAG = "CachedRequestLoader";
    // Mainly for testing purposes.
    private boolean delayCacheLoad = false;
    private boolean delayNetworkLoad = false;

    public BackgroundCachedRequestLoader(Context context, String subscriptionKey, String subscriptionUrl, String userAgent, int socketTag, boolean forceNetwork) {
        requestLoaderDelegation = new RequestLoaderDelegation(context, subscriptionKey, subscriptionUrl, userAgent, socketTag, forceNetwork, this);
    }

    public BackgroundCachedRequestLoader(Context context, String subscriptionKey, String subscriptionUrl, String userAgent, int socketTag) {
        this(context, subscriptionKey, subscriptionUrl, userAgent, socketTag, false);
    }

    @VisibleForTesting
    BackgroundCachedRequestLoader(Context context, String subscriptionKey, String subscriptionUrl, String userAgent, int socketTag, boolean delayCacheLoad, boolean delayNetworkLoad) {
        this(context, subscriptionKey, subscriptionUrl, userAgent, socketTag);
        this.delayCacheLoad = delayCacheLoad;
        this.delayNetworkLoad = delayNetworkLoad;
    }

    public ResponseData getStringLiveData() {
        return requestLoaderDelegation.getStringLiveData();
    }

    public void loadFromCache(Context context, String subscriptionKey, ResponseData stringLiveData) {
        backgroundExecutorService.submit(() -> {
            try {
                Inject.sleepIfTesting(delayCacheLoad);
                String string = FileUtils.readStringFromFile(new FileUtils.GetCache(new WeakReference<>(context)).get(), subscriptionKey);
                stringLiveData.postValue(new Pair<>(ResponseData.SOURCE_CACHE, string));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to open Cache directory when reading cached banner config");
            }
        });
    }

    public void loadFromRemote(ResponseData stringLiveData, String subscriptionUrl, String userAgent, int socketTag) {
        backgroundExecutorService.submit(() -> {
            TrafficStats.setThreadStatsTag(socketTag);
            try {
                Inject.sleepIfTesting(delayNetworkLoad);
                String string = HttpRequest.get(new URL(subscriptionUrl), userAgent);
                string = string.replace("\n", "");
                stringLiveData.postValue(new Pair<>(ResponseData.SOURCE_NETWORK, string));
                if (TextUtils.isEmpty(string)) {
                    requestLoaderDelegation.deleteCache();
                } else {
                    requestLoaderDelegation.writeToCache(string);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
    }

    public void writeToCache(String string, Context context, String subscriptionKey) {
        try {
            final Runnable runnable = new FileUtils.WriteStringToFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), subscriptionKey), string);
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to open cache directory when writing to cache.");
        }
    }

    public void deleteCache(Context context, String subscriptionKey) {
        try {
            final Runnable runnable = new FileUtils.DeleteFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), subscriptionKey));
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to open cache directory when deleting cache.");
        }
    }
}
