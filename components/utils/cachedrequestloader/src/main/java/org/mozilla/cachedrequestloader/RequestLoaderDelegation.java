package org.mozilla.cachedrequestloader;

import android.content.Context;

public class RequestLoaderDelegation {
    private Context context;
    private String subscriptionKey;
    private String subscriptionUrl;
    private String userAgent;
    private int socketTag;
    private ResponseData stringLiveData;
    private boolean forceNetwork;
    private RequestLoader requestLoader;

    RequestLoaderDelegation(Context context, String subscriptionKey, String subscriptionUrl, String userAgent, int socketTag, RequestLoader requestLoader) {
        this(context, subscriptionKey, subscriptionUrl, userAgent, socketTag, false, requestLoader);
    }

    RequestLoaderDelegation(Context context, String subscriptionKey, String subscriptionUrl, String userAgent, int socketTag, boolean forceNetwork, RequestLoader requestLoader) {
        this.context = context;
        this.subscriptionKey = subscriptionKey;
        this.subscriptionUrl = subscriptionUrl;
        this.userAgent = userAgent;
        this.socketTag = socketTag;
        this.forceNetwork = forceNetwork;
        this.requestLoader = requestLoader;
    }

    ResponseData getStringLiveData() {
        if (stringLiveData == null) {
            stringLiveData = new ResponseData();
            if (!forceNetwork) {
                requestLoader.loadFromCache(context, subscriptionKey, stringLiveData);
            }
            requestLoader.loadFromRemote(stringLiveData, subscriptionUrl, userAgent, socketTag);
        }
        return stringLiveData;
    }

    void writeToCache(String string) {
        requestLoader.writeToCache(string, context, subscriptionKey);
    }

    void deleteCache() {
        requestLoader.deleteCache(context, subscriptionKey);
    }

    interface RequestLoader {
        void loadFromCache(Context context, String subscriptionKey, ResponseData stringLiveData);

        void loadFromRemote(ResponseData stringLiveData, String subscriptionUrl, String userAgent, int socketTag);

        void writeToCache(String string, Context context, String subscriptionKey);

        void deleteCache(Context context, String subscriptionKey);
    }
}
