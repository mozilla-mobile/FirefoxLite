package org.mozilla.focus.history;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.provider.QueryHandler.*;

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

    public void insert(Site site, AsyncInsertListener listener) {
        mQueryHandler.startInsert(QueryHandler.SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, QueryHandler.getContentValuesFromSite(site));
    }

    public void delete(long id, AsyncDeleteListener listener) {
        mQueryHandler.startDelete(QueryHandler.SITE_TOKEN, new AsyncDeleteWrapper(id, listener), BrowsingHistory.CONTENT_URI, BrowsingHistory._ID + " = ?", new String[] {Long.toString(id)});
    }

    public void deleteAll(AsyncDeleteListener listener) {
        mQueryHandler.startDelete(QueryHandler.SITE_TOKEN, new AsyncDeleteWrapper(-1, listener), BrowsingHistory.CONTENT_URI, "1", null);
    }

    public void update(Site site, AsyncUpdateListener listener) {
        mQueryHandler.startUpdate(QueryHandler.SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, QueryHandler.getContentValuesFromSite(site), BrowsingHistory._ID + " = ?", new String[] {Long.toString(site.getId())});
    }

    public void query(int offset, int limit, AsyncQueryListener listener) {
        mQueryHandler.startQuery(QueryHandler.SITE_TOKEN, listener, Uri.parse(BrowsingHistory.CONTENT_URI.toString() + "?offset=" + offset + "&limit=" + limit), null, null, null, BrowsingHistory.LAST_VIEW_TIMESTAMP + " DESC");
    }

    public void queryTopSites(int limit, int minViewCount, AsyncQueryListener listener) {
        mQueryHandler.startQuery(QueryHandler.SITE_TOKEN, listener, Uri.parse(BrowsingHistory.CONTENT_URI.toString() + "?limit=" + limit), null, BrowsingHistory.VIEW_COUNT + " >= ?", new String[] {Integer.toString(minViewCount)}, BrowsingHistory.VIEW_COUNT + " DESC");
    }
}
