package org.mozilla.rocket.bhaskar;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.cachedrequestloader.CachedRequestLoader;
import org.mozilla.cachedrequestloader.ResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Repository {
    private int currentPage = FIRST_PAGE;
    private PageSubscription currentPageSubscription;
    private static final String SUBSCRIPTION_KEY_NAME = "bhaskar";
    private static final String DEFAULT_SUBSCRIPTION_URL = "http://appfeed.bhaskar.com/webfeed/apidata/firefox?pageSize=%d&channel_slno=%d&pageNumber=%d";
    private final String subscriptionUrl;
    private static final int FIRST_PAGE = 1;
    private Context context;
    private int channel;
    private int pageSize;
    private String userAgent;
    private int socketTag;
    private OnDataChangedListener onDataChangedListener;
    private OnCacheInvalidateListener onCacheInvalidateListener;
    private List<ItemPojo> itemPojoList;
    private boolean cacheIsDirty = false;

    Repository(Context context, int channel, int pageSize, String userAgent, int socketTag, OnDataChangedListener onDataChangedListener, OnCacheInvalidateListener onCacheInvalidateListener, String subscriptionUrl) {
        this.context = context;
        this.channel = channel;
        this.pageSize = pageSize;
        this.onDataChangedListener = onDataChangedListener;
        this.onCacheInvalidateListener = onCacheInvalidateListener;
        this.subscriptionUrl = subscriptionUrl;
        this.userAgent = userAgent;
        this.socketTag = socketTag;
        itemPojoList = new ArrayList<>();
        nextSubscription();
    }

    Repository(Context context, int channel, int pageSize, String userAgent, int socketTag, OnDataChangedListener onDataChangedListener, OnCacheInvalidateListener onCacheInvalidateListener) {
        this(context, channel, pageSize, userAgent, socketTag, onDataChangedListener, onCacheInvalidateListener, DEFAULT_SUBSCRIPTION_URL);
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

    private CachedRequestLoader createLoader(int page) {
        return new CachedRequestLoader(context, getSubscriptionKey(page), getSubscriptionUrl(page), userAgent, socketTag, cacheIsDirty);
    }

    private Observer<Pair<Integer, String>> createObserver(LiveData<Pair<Integer, String>> liveData, int page) {
        return new Observer<Pair<Integer, String>>() {

            private List<ItemPojo> lastValue;

            @Override
            public void onChanged(@Nullable Pair<Integer, String> integerStringPair) {
                if (integerStringPair == null) {
                    return;
                }
                if (integerStringPair.first != null) {
                    if (TextUtils.isEmpty(integerStringPair.second)) {
                        return;
                    }
                    try {
                        List<ItemPojo> itemPojoList = Repository.this.parseData(integerStringPair.second);
                        // Removes the subscription and mark as done once network returns.
                        if (integerStringPair.first == ResponseData.SOURCE_NETWORK) {
                            if (lastValue != null) {
                                List<ItemPojo> diff = new ArrayList<>(lastValue);
                                diff.removeAll(itemPojoList);
                                cacheIsDirty = diff.size() == 0;
                                if (cacheIsDirty && onCacheInvalidateListener != null) {
                                    onCacheInvalidateListener.onCacheInvalidate();
                                }
                            }
                            liveData.removeObserver(this);
                            currentPageSubscription = null;
                        }
                        Repository.this.updateData(page, itemPojoList);
                        lastValue = itemPojoList;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void updateData(int page, List<ItemPojo> newItems) {
        if (page == FIRST_PAGE) {
            itemPojoList.clear();
        }
        itemPojoList.addAll(newItems);
        this.onDataChangedListener.onDataChanged(itemPojoList);
    }

    private List<ItemPojo> parseData(String response) throws JSONException {
        List<ItemPojo> ret = new ArrayList<>();
        // TODO: 11/2/18 It takes 0.1s - 0.2s to create JsonObject, do we want to improve this?
        JSONObject root = new JSONObject(response);
        JSONObject data = root.getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        for (int i = 0 ; i < rows.length() ; i++) {
            JSONObject row = rows.getJSONObject(i);
            ItemPojo itemPojo = new ItemPojo();
            itemPojo.id = row.getString("id");
            itemPojo.articleFrom = row.getString("articleFrom");
            itemPojo.category = row.getString("category");
            itemPojo.city = row.getString("city");
            itemPojo.coverPic = row.getString("coverPic");
            itemPojo.description = row.getString("description");
            itemPojo.detailUrl = row.getString("detailUrl");
            itemPojo.keywords = row.getString("keywords");
            itemPojo.language = row.getString("language");
            itemPojo.province = row.getString("province");
            itemPojo.publishTime = row.getInt("publishTime");
            itemPojo.subcategory = row.getString("subcategory");
            itemPojo.summary = row.getString("summary");
            String separator = "" + '\0';
            itemPojo.tags = row.getJSONArray("tags").join(separator).split(separator);
            itemPojo.title = row.getString("title");
            ret.add(itemPojo);
        }
        return ret;
    }

    private String getSubscriptionKey(int page) {
        return SUBSCRIPTION_KEY_NAME + pageSize + "/" + page;
    }

    private String getSubscriptionUrl(int pageNumber) {
        return String.format(Locale.US, subscriptionUrl, pageSize, channel, pageNumber);
    }

    public interface OnDataChangedListener {
        void onDataChanged(List<ItemPojo> itemPojoList);
    }

    public interface OnCacheInvalidateListener {
        void onCacheInvalidate();
    }

    private static class PageSubscription {
        private int page;
        private CachedRequestLoader cachedRequestLoader;
        private Observer<Pair<Integer, String>> observer;
    }
}