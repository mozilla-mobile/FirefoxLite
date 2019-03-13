package org.mozilla.lite.partner;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import org.json.JSONException;
import org.mozilla.cachedrequestloader.BackgroundCachedRequestLoader;
import org.mozilla.cachedrequestloader.ResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Repository<T extends NewsItem> {
    private PageSubscription currentPageSubscription;
    private final String subscriptionKeyName;
    private final int firstPage;
    private int currentPage;
    private Context context;
    private String userAgent;
    private int socketTag;
    @Nullable private OnDataChangedListener<T> onDataChangedListener;
    private OnCacheInvalidateListener onCacheInvalidateListener;
    private List<T> itemPojoList;
    private boolean cacheIsDirty = false;
    private Parser<T> parser;
    private boolean dedup;

    public Repository(Context context, String userAgent, int socketTag, OnDataChangedListener onDataChangedListener, OnCacheInvalidateListener onCacheInvalidateListener, String subscriptionKeyName, int firstPage, Parser<T> parser, boolean dedup) {
        this.context = context;
        this.onDataChangedListener = onDataChangedListener;
        this.onCacheInvalidateListener = onCacheInvalidateListener;
        this.subscriptionKeyName = subscriptionKeyName;
        this.firstPage = firstPage;
        currentPage = firstPage;
        this.userAgent = userAgent;
        this.socketTag = socketTag;
        this.parser = parser;
        this.dedup = dedup;
        itemPojoList = new ArrayList<>();
        nextSubscription();
    }

    public void loadMore() {
        nextSubscription();
    }

    // Not used yet and not tested yet.
    public void reloadData() {
        itemPojoList = new ArrayList<>();
        cacheIsDirty = true;
        nextSubscription();
    }

    public void setOnDataChangedListener(@Nullable OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    public void reset() {
        itemPojoList = new ArrayList<>();
        cacheIsDirty = true;
        this.onDataChangedListener = null;
    }

    private void nextSubscription() {
        if (currentPageSubscription != null) {
            return;
        }
        PageSubscription ret = new PageSubscription();
        ret.page = currentPage;
        ret.cachedRequestLoader = createLoader(ret.page);
        currentPage++;
        ResponseData responseData = ret.cachedRequestLoader.getStringLiveData();
        ret.observer = createObserver(responseData, ret.page);
        responseData.observeForever(ret.observer);
        currentPageSubscription = ret;
    }

    private BackgroundCachedRequestLoader createLoader(int page) {
        return new BackgroundCachedRequestLoader(context, getSubscriptionKey(page), getSubscriptionUrl(page), userAgent, socketTag, cacheIsDirty);
    }

    private Observer<Pair<Integer, String>> createObserver(LiveData<Pair<Integer, String>> liveData, int page) {
        return new Observer<Pair<Integer, String>>() {

            private List<T> lastValue;

            private void removeObserver() {
                liveData.removeObserver(this);
                currentPageSubscription = null;
            }

            /**
             * @param oldValuesFromCache
             * @param newValuesFromNetwork
             * @return if the cache is found dirty during this check
             */
            private boolean updateCacheStatus(List<T> oldValuesFromCache, List<T> newValuesFromNetwork) {
                if (oldValuesFromCache == null) {
                    return false;
                }
                List<T> diff = new ArrayList<>(oldValuesFromCache);
                diff.removeAll(newValuesFromNetwork);
                boolean cacheIsDirty = diff.size() != 0;
                if (cacheIsDirty && onCacheInvalidateListener != null) {
                    onCacheInvalidateListener.onCacheInvalidate();
                }
                return cacheIsDirty;
            }

            @Override
            public void onChanged(@Nullable Pair<Integer, String> integerStringPair) {
                if (integerStringPair == null || integerStringPair.first == null) {
                    return;
                }
                // Parse the data, from cache or from Network
                if (!TextUtils.isEmpty(integerStringPair.second)) {
                    try {
                        List<T> itemPojoList = parser.parse(integerStringPair.second);
                        if (!cacheIsDirty) {
                            cacheIsDirty = updateCacheStatus(lastValue, itemPojoList);
                            if (cacheIsDirty && integerStringPair.first == ResponseData.SOURCE_NETWORK) {
                                Repository.this.correctData(lastValue, itemPojoList);
                                removeObserver();
                                return;
                            }
                        }
                        if (integerStringPair.first == ResponseData.SOURCE_NETWORK || !cacheIsDirty) {
                            Repository.this.addData(page, itemPojoList);
                            lastValue = itemPojoList;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // if result is null, means it's first observation, ignore that event
                    // but if the result is "", means network request complete with no
                    // data, so still notify the observer means nothing happened since
                    // last fetched.
                    // TODO: use network failure callback instead
                    if (integerStringPair.first == ResponseData.SOURCE_NETWORK && "".equals(integerStringPair.second)) {
                        if (onDataChangedListener != null) {
                            onDataChangedListener.onDataChanged(cloneData());
                        }
                    }
                }
                // Removes the subscription and mark as done once network returns, no matter
                // if there is a Network error or not.
                if (integerStringPair.first == ResponseData.SOURCE_NETWORK) {
                    removeObserver();
                }
            }
        };
    }

    private List<T> cloneData() {
        return Collections.unmodifiableList(new ArrayList<>(itemPojoList));
    }

    private void correctData(List<T> oldItems, List<T> newItems) {
        itemPojoList.removeAll(oldItems);
        addData(false, newItems);
        if (this.onDataChangedListener != null) {
            this.onDataChangedListener.onDataChanged(itemPojoList);
        }
    }

    private void addData(int page, List<T> newItems) {
        addData(page == firstPage, newItems);
    }

    private void addData(boolean reset, List<T> newItems) {
        if (reset) {
            itemPojoList.clear();
        }
        if (dedup) {
            newItems.removeAll(itemPojoList);
        }
        itemPojoList.addAll(newItems);
        if (this.onDataChangedListener != null) {
            this.onDataChangedListener.onDataChanged(cloneData());
        }
    }

    private String getSubscriptionKey(int page) {
        return subscriptionKeyName + "/" + page;
    }

    abstract protected String getSubscriptionUrl(int pageNumber);

    public interface OnDataChangedListener<T> {
        void onDataChanged(List<T> itemPojoList);
    }

    public interface OnCacheInvalidateListener {
        void onCacheInvalidate();
    }

    private static class PageSubscription {
        private int page;
        private BackgroundCachedRequestLoader cachedRequestLoader;
        private Observer<Pair<Integer, String>> observer;
    }

    public interface Parser<T extends NewsItem> {
        List<T> parse(String source) throws JSONException;
    }
}