package org.mozilla.focus.activity;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;

class BrowserMediator {

    private final MainActivity activity;
    private final MainMediator mainMediator;

    BrowserMediator(@NonNull MainActivity activity, @NonNull MainMediator mainMediator) {
        this.activity = activity;
        this.mainMediator = mainMediator;
    }

    // A.k.a. close Home Screen
    void raiseBrowserScreen(boolean animate) {
        mainMediator.clearAllFragment(animate);
        this.activity.sendBrowsingTelemetry();
    }

    void showBrowserScreen(@NonNull String url, boolean openInNewTab) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        findBrowserFragment(fragmentManager).loadUrl(url, openInNewTab, new Runnable() {
            @Override
            public void run() {
                raiseBrowserScreen(true);
            }
        });
    }

    void showBrowserScreenForRestoreTabs(@NonNull String tabId) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        findBrowserFragment(fragmentManager).loadTab(tabId);
        raiseBrowserScreen(false);
    }


    private BrowserFragment findBrowserFragment(FragmentManager fm) {
        return (BrowserFragment) fm.findFragmentById(R.id.browser);
    }
}
