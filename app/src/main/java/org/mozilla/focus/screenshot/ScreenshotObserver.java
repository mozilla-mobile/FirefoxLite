package org.mozilla.focus.screenshot;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.mozilla.focus.utils.ThreadUtils;

public class ScreenshotObserver {
    public Context context;

    /**
     * Listener for screenshot changes.
     */
    public interface OnScreenshotListener {
        /**
         * This callback is executed on the UI thread.
         */
        void onScreenshotTaken(String data, String title);
    }

    private OnScreenshotListener listener;

    public ScreenshotObserver(Context context, OnScreenshotListener listener) {
        this.context = context;
        this.listener = listener;
    }

    private MediaObserver mediaObserver;
    private String[] mediaProjections = new String[] {
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.TITLE
    };

    /**
     * Start ScreenshotObserver if this device is supported and all required runtime permissions
     * have been granted by the user. Calling this method will not prompt for permissions.
     */
    public void start() {
        ThreadUtils.postToBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mediaObserver == null) {
                        mediaObserver = new MediaObserver();
                        context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, mediaObserver);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() {
        if (mediaObserver == null) {
            return;
        }

        try {
            context.getContentResolver().unregisterContentObserver(mediaObserver);
            mediaObserver = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMediaChange(final Uri uri) {
        // Make sure we are on not on the main thread.
        final ContentResolver cr = context.getContentResolver();
        ThreadUtils.postToBackgroundThread(new Runnable() {
            @Override
            public void run() {
                // Find the most recent image modification to the MediaStore and see if it's a screenshot.
                final Cursor cursor = cr.query(uri, mediaProjections, null, null, MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC LIMIT 1");
                try {
                    if (cursor == null) {
                        return;
                    }

                    while (cursor.moveToNext()) {
                        String data = cursor.getString(0);
                        String album = cursor.getString(1);
                        String title = cursor.getString(2);
                        if (album != null && album.toLowerCase().contains("screenshot")) {
                            if (listener != null) {
                                listener.onScreenshotTaken(data, title);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    private class MediaObserver extends ContentObserver {
        public MediaObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            onMediaChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
    }
}
