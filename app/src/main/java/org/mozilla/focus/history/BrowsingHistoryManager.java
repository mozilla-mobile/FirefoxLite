/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.history;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.provider.HistoryContract;
import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.provider.QueryHandler.AsyncDeleteListener;
import org.mozilla.focus.provider.QueryHandler.AsyncDeleteWrapper;
import org.mozilla.focus.provider.QueryHandler.AsyncInsertListener;
import org.mozilla.focus.provider.QueryHandler.AsyncQueryListener;
import org.mozilla.focus.provider.QueryHandler.AsyncUpdateListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hart on 07/08/2017.
 */

public class BrowsingHistoryManager {

    private static BrowsingHistoryManager sInstance;

    private WeakReference<ContentResolver> mResolver;
    private QueryHandler mQueryHandler;
    private BrowsingHistoryContentObserver mContentObserver;
    private ArrayList<ContentChangeListener> mListeners;

    private final class BrowsingHistoryContentObserver extends ContentObserver {

        public BrowsingHistoryContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            for (ContentChangeListener listener : mListeners) {
                listener.onContentChanged();
            }
        }
    }

    public interface ContentChangeListener {
        void onContentChanged();
    }

    public static BrowsingHistoryManager getInstance() {
        if (sInstance == null) {
            sInstance = new BrowsingHistoryManager();
        }
        return sInstance;
    }

    public void init(Context context) {
        ContentResolver resolver = context.getContentResolver();
        mResolver = new WeakReference<>(resolver);
        mQueryHandler = new QueryHandler(resolver);
        mContentObserver = new BrowsingHistoryContentObserver(null);
        mListeners = new ArrayList<>();
    }

    public void registerContentChangeListener(ContentChangeListener listener) {
        if (listener == null) {
            return;
        }

        mListeners.add(listener);
        if (mListeners.size() == 1) {
            ContentResolver resolver = mResolver.get();
            if (resolver != null) {
                resolver.registerContentObserver(BrowsingHistory.CONTENT_URI, false, mContentObserver);
            }
        }
    }

    public void unregisterContentChangeListener(ContentChangeListener listener) {
        if (listener == null) {
            return;
        }

        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            ContentResolver resolver = mResolver.get();
            if (resolver != null) {
                resolver.unregisterContentObserver(mContentObserver);
            }
        }
    }

    public void insert(final Site site, final AsyncInsertListener listener) {
        mQueryHandler.postWorker(new Runnable() {
            @Override
            public void run() {
                final ContentValues contentValues = QueryHandler.getContentValuesFromSite(site);
                mQueryHandler.startInsert(QueryHandler.SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, contentValues);
            }
        });
    }

    public void delete(long id, AsyncDeleteListener listener) {
        mQueryHandler.startDelete(QueryHandler.SITE_TOKEN, new AsyncDeleteWrapper(id, listener), BrowsingHistory.CONTENT_URI, BrowsingHistory._ID + " = ?", new String[]{Long.toString(id)});
    }

    public void deleteAll(AsyncDeleteListener listener) {
        mQueryHandler.startDelete(QueryHandler.SITE_TOKEN, new AsyncDeleteWrapper(-1, listener), BrowsingHistory.CONTENT_URI, "1", null);
    }

    public void updateLastEntry(final Site site, final AsyncUpdateListener listener) {
        mQueryHandler.postWorker(new Runnable() {
            @Override
            public void run() {
                final ContentValues contentValues = QueryHandler.getContentValuesFromSite(site);
                mQueryHandler.startUpdate(QueryHandler.SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, contentValues, BrowsingHistory._ID + " = ( SELECT " + BrowsingHistory._ID + " FROM " + HistoryContract.TABLE_NAME + " WHERE " + BrowsingHistory.URL + " = ? ORDER BY " + BrowsingHistory.LAST_VIEW_TIMESTAMP + " DESC)", new String[]{site.getUrl()});
            }
        });
    }

    public void query(int offset, int limit, AsyncQueryListener listener) {
        mQueryHandler.startQuery(QueryHandler.SITE_TOKEN, listener, Uri.parse(BrowsingHistory.CONTENT_URI.toString() + "?offset=" + offset + "&limit=" + limit), null, null, null, BrowsingHistory.LAST_VIEW_TIMESTAMP + " DESC");
    }

    public void queryTopSites(int limit, int minViewCount, AsyncQueryListener listener) {
        mQueryHandler.startQuery(QueryHandler.SITE_TOKEN, listener, Uri.parse(BrowsingHistory.CONTENT_URI.toString() + "?limit=" + limit), null, BrowsingHistory.VIEW_COUNT + " >= ?", new String[]{Integer.toString(minViewCount)}, BrowsingHistory.VIEW_COUNT + " DESC");
    }
}
