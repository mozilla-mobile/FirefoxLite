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
    void raiseBrowserScreen(boolean animate, boolean fromTabTray) {
        if (fromTabTray) {
            findBrowserFragment(this.activity.getSupportFragmentManager()).setNoSwitchTabAfterNewIntent(false);
        }
        mainMediator.clearAllFragment(animate);
        this.activity.sendBrowsingTelemetry();
    }

    void showBrowserScreen(@NonNull String url, boolean openInNewTab, boolean isFromExternal) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        findBrowserFragment(fragmentManager).loadUrl(url, openInNewTab, isFromExternal, new Runnable() {
            @Override
            public void run() {
                raiseBrowserScreen(true, false);
            }
        });
    }

    void showBrowserScreenForRestoreTabs(@NonNull String tabId) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        findBrowserFragment(fragmentManager).loadTab(tabId);
        raiseBrowserScreen(false, false);
    }


    private BrowserFragment findBrowserFragment(FragmentManager fm) {
        return (BrowserFragment) fm.findFragmentById(R.id.browser);
    }
}
