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
import org.mozilla.focus.utils.Settings;

import java.lang.ref.WeakReference;

public class CaptureRunnable extends ScreenshotCaptureTask implements Runnable, BrowserFragment.ScreenshotCallback {

    final WeakReference<Context> refContext;
    final WeakReference<BrowserFragment> refBrowserFragment;
    final WeakReference<ScreenCaptureDialogFragment> refScreenCaptureDialogFragment;
    final WeakReference<View> refContainerView;

    public interface CaptureStateListener {
        void onPromptScreenshotResult();
    }


    public CaptureRunnable(Context context, BrowserFragment browserFragment, ScreenCaptureDialogFragment screenCaptureDialogFragment, View container) {
        super(context);
        refContext = new WeakReference<>(context);
        refBrowserFragment = new WeakReference<>(browserFragment);
        refScreenCaptureDialogFragment = new WeakReference<>(screenCaptureDialogFragment);
        refContainerView = new WeakReference<>(container);
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
            promptScreenshotResult(false);
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
        final boolean captureSuccess = !TextUtils.isEmpty(path);
        if (captureSuccess) {
            Settings.getInstance(refContext.get()).setHasUnreadMyShot(true);
        }
        screenCaptureDialogFragment.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                promptScreenshotResult(captureSuccess);
            }
        });
        if (TextUtils.isEmpty(path)) {
            screenCaptureDialogFragment.dismiss();
        } else {
            screenCaptureDialogFragment.dismiss(true);
        }
    }

    private void promptScreenshotResult(final boolean success) {
        Context context = refContext.get();
        if (context == null) {
            return;
        }

        if (refBrowserFragment != null) {
            final BrowserFragment browserFragment = refBrowserFragment.get();
            if (browserFragment != null && browserFragment.getCaptureStateListener() != null) {
                browserFragment.getCaptureStateListener().onPromptScreenshotResult();
            }

            if (browserFragment != null && success
                    && !Settings.getInstance(context).getEventHistory().contains(Settings.Event.ShowMyShotOnBoardingDialog)) {
                // My shot on boarding didn't show before and capture is succeed, skip to show toast
                browserFragment.showMyShotOnBoarding();
                return;
            }
        }
        Toast.makeText(context, success ? R.string.screenshot_saved : R.string.screenshot_failed, Toast.LENGTH_SHORT).show();
    }

}
