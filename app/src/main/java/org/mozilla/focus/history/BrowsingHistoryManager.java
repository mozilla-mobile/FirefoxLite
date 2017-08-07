package org.mozilla.focus.history;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;

/**
 * Created by hart on 07/08/2017.
 */

public class BrowsingHistoryManager {

    private static final int SITE_TOKEN = 1;

    private static BrowsingHistoryManager sInstance;

    private BrowsingHistoryQueryHandler mQueryHandler;

    private static final class BrowsingHistoryQueryHandler extends AsyncQueryHandler {

        public BrowsingHistoryQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            switch (token) {
                case SITE_TOKEN:
                    if (cookie != null) {
                        ((AsyncInsertListener) cookie).onInsertComplete(uri);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    public interface AsyncInsertListener {
        void onInsertComplete(Uri uri);
    }

    public static BrowsingHistoryManager getInstance() {
        if (sInstance == null) {
            sInstance = new BrowsingHistoryManager();
        }
        return sInstance;
    }

    public void init(Context context) {
        mQueryHandler = new BrowsingHistoryQueryHandler(context);
    }

    public void insert(Site site, AsyncInsertListener listener) {
        ContentValues values = new ContentValues();
        values.put(BrowsingHistory.TITLE, site.getTitle());
        values.put(BrowsingHistory.URL, site.getUrl());
        values.put(BrowsingHistory.LAST_VIEW_TIMESTAMP, site.getLastViewTimestamp());
        if (site.getFavIcon() != null) {
            values.put(BrowsingHistory.FAV_ICON, site.getFavIconInBytes());
        }
        mQueryHandler.startInsert(SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, values);
    }
}
