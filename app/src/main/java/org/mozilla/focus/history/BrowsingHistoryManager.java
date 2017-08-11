package org.mozilla.focus.history;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;

import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by hart on 07/08/2017.
 */

public class BrowsingHistoryManager {

    private static final int SITE_TOKEN = 1;

    private static BrowsingHistoryManager sInstance;

    private Context mContext;
    private BrowsingHistoryQueryHandler mQueryHandler;
    private BrowsingHistoryContentObserver mContentObserver;
    private ArrayList<ContentChangeListener> mListeners;

    private static final class BrowsingHistoryQueryHandler extends AsyncQueryHandler {

        public BrowsingHistoryQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            switch (token) {
                case SITE_TOKEN:
                    if (cookie != null) {
                        final int id = uri == null ? -1 : Integer.parseInt(uri.getLastPathSegment());
                        ((AsyncInsertListener) cookie).onInsertComplete(id);
                    }
                    break;
                default:
                    // do nothing
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
                case SITE_TOKEN:
                    if (cookie != null) {
                        ((AsyncDeleteListener) cookie).onDeleteComplete(result);
                    }
                    break;
                default:
                    // do nothing
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            switch (token) {
                case SITE_TOKEN:
                    if (cookie != null) {
                        ((AsyncUpdateListener) cookie).onUpdateComplete(result);
                    }
                    break;
                default:
                    // do nothing
            }
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case SITE_TOKEN:
                    if (cookie != null) {
                        ((AsyncQueryListener) cookie).onQueryComplete(cursor);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

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

    public interface AsyncInsertListener {
        void onInsertComplete(int id);
    }

    public interface AsyncDeleteListener {
        void onDeleteComplete(int result);
    }

    public interface AsyncUpdateListener {
        void onUpdateComplete(int result);
    }

    public interface AsyncQueryListener {
        void onQueryComplete(Cursor cursor);
    }

    public static BrowsingHistoryManager getInstance() {
        if (sInstance == null) {
            sInstance = new BrowsingHistoryManager();
        }
        return sInstance;
    }

    public void init(Context context) {
        mContext = context;
        mQueryHandler = new BrowsingHistoryQueryHandler(context);
        mContentObserver = new BrowsingHistoryContentObserver(null);
        mListeners = new ArrayList<>();
    }

    public void registerContentChangeListener(ContentChangeListener listener) {
        if (listener == null) {
            return;
        }

        mListeners.add(listener);
        if (mListeners.size() == 1) {
            mContext.getContentResolver().registerContentObserver(BrowsingHistory.CONTENT_URI, false, mContentObserver);
        }
    }

    public void unregisterContentChangeListener(ContentChangeListener listener) {
        if (listener == null) {
            return;
        }

        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        }
    }

    public void insert(Site site, AsyncInsertListener listener) {
        mQueryHandler.startInsert(SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, getContentValuesFromSite(site));
    }

    public void delete(long id, AsyncDeleteListener listener) {
        mQueryHandler.startDelete(SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, BrowsingHistory._ID + " = ?", new String[] {Long.toString(id)});
    }

    public void deleteAll(AsyncDeleteListener listener) {
        mQueryHandler.startDelete(SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, "1", null);
    }

    public void update(Site site, AsyncUpdateListener listener) {
        mQueryHandler.startUpdate(SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, getContentValuesFromSite(site), BrowsingHistory._ID + " = ?", new String[] {Long.toString(site.getId())});
    }

    public void query(AsyncQueryListener listener) {
        mQueryHandler.startQuery(SITE_TOKEN, listener, BrowsingHistory.CONTENT_URI, null, null, null, BrowsingHistory.LAST_VIEW_TIMESTAMP + " DESC");
    }

    public void queryTopSites(int limit, int minViewCount, AsyncQueryListener listener) {
        mQueryHandler.startQuery(SITE_TOKEN, listener, Uri.parse(BrowsingHistory.CONTENT_URI.toString() + "?limit=" + limit), null, BrowsingHistory.VIEW_COUNT + " >= ?", new String[] {Integer.toString(minViewCount)}, BrowsingHistory.VIEW_COUNT + " DESC");
    }

    public static Site cursorToSite(Cursor cursor) {
        Site site = new Site();
        site.setId(cursor.getLong(cursor.getColumnIndex(BrowsingHistory._ID)));
        site.setTitle(cursor.getString(cursor.getColumnIndex(BrowsingHistory.TITLE)));
        site.setUrl(cursor.getString(cursor.getColumnIndex(BrowsingHistory.URL)));
        site.setViewCount(cursor.getLong(cursor.getColumnIndex(BrowsingHistory.VIEW_COUNT)));
        site.setLastViewTimestamp(cursor.getLong(cursor.getColumnIndex(BrowsingHistory.LAST_VIEW_TIMESTAMP)));
        site.setFavIcon(bytesToBitmap(cursor.getBlob(cursor.getColumnIndex(BrowsingHistory.FAV_ICON))));
        return site;
    }

    private static ContentValues getContentValuesFromSite(Site site) {
        ContentValues values = new ContentValues();
        if (site.getTitle() != null) values.put(BrowsingHistory.TITLE, site.getTitle());
        if (site.getUrl() != null) values.put(BrowsingHistory.URL, site.getUrl());
        if (site.getViewCount() != 0) values.put(BrowsingHistory.VIEW_COUNT, site.getViewCount());
        if (site.getLastViewTimestamp() != 0) values.put(BrowsingHistory.LAST_VIEW_TIMESTAMP, site.getLastViewTimestamp());
        if (site.getFavIcon() != null) values.put(BrowsingHistory.FAV_ICON, bitmapToBytes(site.getFavIcon()));
        return values;
    }

    private static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private static Bitmap bytesToBitmap(byte[] data) {
        return data == null ? null : BitmapFactory.decodeByteArray(data, 0, data.length);
    }
}
