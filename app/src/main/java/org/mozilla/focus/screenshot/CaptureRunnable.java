package org.mozilla.focus.screenshot;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.ScreenCaptureDialogFragment;

import java.lang.ref.WeakReference;

public class CaptureRunnable extends ScreenshotCaptureTask implements Runnable, BrowserFragment.ScreenshotCallback {

    final WeakReference<Context> refContext;
    final WeakReference<BrowserFragment> refBrowserFragment;
    final WeakReference<ScreenCaptureDialogFragment> refScreenCaptureDialogFragment;
    final WeakReference<View> refContainerView;
    private static volatile boolean completed;

    public CaptureRunnable(Context context, BrowserFragment browserFragment, ScreenCaptureDialogFragment screenCaptureDialogFragment, View container) {
        super(context);
        refContext = new WeakReference<>(context);
        refBrowserFragment = new WeakReference<>(browserFragment);
        refScreenCaptureDialogFragment = new WeakReference<>(screenCaptureDialogFragment);
        refContainerView = new WeakReference<>(container);
        setCompleted(false);
    }

    public static boolean isCompleted() {
        return completed;
    }

    public static void setCompleted(boolean completed) {
        CaptureRunnable.completed = completed;
    }

    @Override
    public void run() {
        BrowserFragment browserFragment = refBrowserFragment.get();
        if (browserFragment == null) {
            return;
        }
        if (browserFragment.capturePage(this)) {
            //  onCaptureComplete called
        } else {
            //  Capture failed
            ScreenCaptureDialogFragment screenCaptureDialogFragment = refScreenCaptureDialogFragment.get();
            if (screenCaptureDialogFragment != null) {
                screenCaptureDialogFragment.dismiss();
            }
            promptScreenshotResult(R.string.screenshot_failed);
        }
    }

    @Override
    public void onCaptureComplete(String title, String url, Bitmap bitmap) {
        Context context = refContext.get();
        if (context == null) {
            return;
        }

        execute(title, url, bitmap);
    }

    @Override
    protected void onPostExecute(final String path) {
        ScreenCaptureDialogFragment screenCaptureDialogFragment = refScreenCaptureDialogFragment.get();
        if (screenCaptureDialogFragment == null) {
            cancel(true);
            return;
        }
        final int captureResultResource = TextUtils.isEmpty(path) ? R.string.screenshot_failed : R.string.screenshot_saved;
        screenCaptureDialogFragment.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                promptScreenshotResult(captureResultResource);
            }
        });
        if (TextUtils.isEmpty(path)) {
            screenCaptureDialogFragment.dismiss();
        } else {
            screenCaptureDialogFragment.dismiss(true);
        }
    }

    private void promptScreenshotResult(int snackbarTitleId) {
        Context context = refContext.get();
        if (context == null) {
            return;
        }
        Toast.makeText(context, snackbarTitleId, Toast.LENGTH_SHORT).show();
        completed = true;
    }

}
