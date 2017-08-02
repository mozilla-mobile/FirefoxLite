/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.DownloadDialogShowListener;
import org.mozilla.focus.widget.FragmentListener;

public class MainActivity extends LocaleAwareAppCompatActivity implements FragmentListener {
    public static final String ACTION_OPEN = "open";

    public static final String EXTRA_TEXT_SELECTION = "text_selection";

    private String pendingUrl;

    private BottomSheetDialog menu;
    private BottomSheetDialog historyAndDownload;
    private FloatingActionButton btnSearch;
    private FloatingActionButton btnHome;
    private FloatingActionButton btnMenu;

    private MainMediator mediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initViews();

        mediator = new MainMediator(this);
        mediator.registerHome(btnHome);
        mediator.registerSearch(btnSearch);
        mediator.registerMenu(btnMenu);

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final String url = intent.getDataString();

                BrowsingSession.getInstance().loadCustomTabConfig(this, intent);

                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    pendingUrl = url;
                    this.mediator.showFirstRun();
                } else {
                    this.mediator.showBrowserScreen(url);
                }
            } else {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    this.mediator.showFirstRun();
                } else {
                    this.mediator.showHomeScreen();
                }
            }
        }

        WebViewProvider.preload(this);
    }

    @Override
    public void applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    @Override
    protected void onStart() {
        // TODO: handle fragment creation
        //HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        //if (homeFragment != null) {
        //    getTopSitesPresenter().setView(homeFragment);
        //}
        //UrlInputFragment urlInputFragment = (UrlInputFragment) getSupportFragmentManager().findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        //if (urlInputFragment != null) {
        //    getUrlInputPresenter().setView(urlInputFragment);
        //}
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        TelemetryWrapper.startSession();
    }

    @Override
    protected void onPause() {
        super.onPause();

        TelemetryWrapper.stopSession();
    }

    @Override
    protected void onStop() {
        super.onStop();

        TelemetryWrapper.stopMainActivity();
    }

    @Override
    protected void onNewIntent(Intent unsafeIntent) {
        final SafeIntent intent = new SafeIntent(unsafeIntent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // We can't update our fragment right now because we need to wait until the activity is
            // resumed. So just remember this URL and load it in onResumeFragments().
            pendingUrl = intent.getDataString();
        }

        if (ACTION_OPEN.equals(intent.getAction())) {
            TelemetryWrapper.openNotificationActionEvent();
        }

        // We do not care about the previous intent anymore. But let's remember this one.
        setIntent(unsafeIntent);
        BrowsingSession.getInstance().loadCustomTabConfig(this, intent);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (pendingUrl != null && !Settings.getInstance(this).shouldShowFirstrun()) {
            // We have received an URL in onNewIntent(). Let's load it now.
            // Unless we're trying to show the firstrun screen, in which case we leave it pending until
            // firstrun is dismissed.
            this.mediator.showBrowserScreen(pendingUrl);
            pendingUrl = null;
        }
    }

    private void initViews() {
        int visibility = getWindow().getDecorView().getSystemUiVisibility();
        // do not overwrite existing value
        visibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(visibility);

        btnSearch = (FloatingActionButton) findViewById(R.id.btn_search);
        btnHome = (FloatingActionButton) findViewById(R.id.btn_home);
        btnMenu = (FloatingActionButton) findViewById(R.id.btn_menu);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.mediator.showUrlInput(null);
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
        setUpMenu();
        setUpHistoryAndDownload();
    }

    private void setUpMenu() {
        final View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_main_menu, null);
        menu = new BottomSheetDialog(this);
        menu.setContentView(sheet);
    }

    private void setUpHistoryAndDownload() {
        final View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_history_download, null);
        historyAndDownload = new BottomSheetDialog(this);
        historyAndDownload.setContentView(sheet);

        DownloadDialogShowListener listener = new DownloadDialogShowListener(sheet);
        historyAndDownload.setOnShowListener(listener);
        historyAndDownload.setOnCancelListener(listener);
        historyAndDownload.setOnDismissListener(listener);
    }

    private void showMenu() {
        menu.show();
    }

    private void showHistoryAndDownload(boolean isHistory) {
        historyAndDownload.show();
    }

    public void onMenuItemClicked(View v) {
        menu.cancel();
        switch (v.getId()) {
            case R.id.menu_download:
                onHistoryClicked();
                break;
            case R.id.menu_history:
                onDownloadClicked();
                break;
            case R.id.menu_preferences:
                onPreferenceClicked();
                break;
            case R.id.action_back:
            case R.id.action_next:
            case R.id.action_refresh:
            case R.id.action_share:
                onMenuBrowsingItemClicked(v);
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuItemClicked() is only for" +
                        " known ids");
        }
    }

    public void onMenuBrowsingItemClicked(View v) {
        final BrowserFragment browserFragment = getBrowserFragment();
        if (browserFragment == null || !browserFragment.isVisible()) {
            return;
        }
        switch (v.getId()) {
            case R.id.action_back:
                onBackClicked(browserFragment);
                break;
            case R.id.action_next:
                onNextClicked(browserFragment);
                break;
            case R.id.action_refresh:
                onRefreshClicked(browserFragment);
                break;
            case R.id.action_share:
                onShraeClicked(browserFragment);
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuBrowsingItemClicked() is" +
                        " only for known ids");
        }
    }

    private void onPreferenceClicked() {
        openPreferences();
    }

    private void onHistoryClicked() {
        showHistoryAndDownload(true);
    }

    private void onDownloadClicked() {
        showHistoryAndDownload(false);
    }

    private BrowserFragment getBrowserFragment() {
        return (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
    }

    private void onBackClicked(final BrowserFragment browserFragment) {
        browserFragment.goBack();
    }

    private void onNextClicked(final BrowserFragment browserFragment) {
        browserFragment.goForward();
    }

    private void onRefreshClicked(final BrowserFragment browserFragment) {
        browserFragment.reload();
    }

    private void onShraeClicked(final BrowserFragment browserFragment) {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, browserFragment.getUrl());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)));

        TelemetryWrapper.shareEvent();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        if (name.equals(IWebView.class.getName())) {
            View v = WebViewProvider.create(this, attrs);
            return v;
        }

        return super.onCreateView(name, context, attrs);
    }

    @Override
    public void onBackPressed() {
        if (this.mediator.handleBackKey()) {
            return;
        }
        super.onBackPressed();
    }

    public void firstrunFinished() {
        if (pendingUrl != null) {
            // We have received an URL in onNewIntent(). Let's load it now.
            this.mediator.showBrowserScreen(pendingUrl);
            pendingUrl = null;
        } else {
            this.mediator.showHomeScreen();
        }
    }

    @Override
    public void onNotified(@NonNull Fragment from, @NonNull TYPE type, @Nullable Object payload) {
        switch (type) {
            case OPEN_URL:
                if ((payload != null) && (payload instanceof String)) {
                    this.mediator.showBrowserScreen(payload.toString());
                }
                break;
            case OPEN_PREFERENCE:
                openPreferences();
                break;
            case SHOW_HOME:
                this.mediator.showHomeScreen();
                break;
            case SHOW_MENU:
                this.showMenu();
                break;
            case SHOW_URL_INPUT:
                final String url = (payload != null) ? payload.toString() : null;
                this.mediator.showUrlInput(url);
                break;
            case DISMISS_URL_INPUT:
                this.mediator.dismissUrlInput();
                break;
            case FRAGMENT_STARTED:
                if ((payload != null) && (payload instanceof String)) {
                    this.mediator.onFragmentStarted(((String) payload).toLowerCase());
                }
                break;
            case FRAGMENT_STOPPED:
                if ((payload != null) && (payload instanceof String)) {
                    this.mediator.onFragmentStopped(((String) payload).toLowerCase());
                }
                break;
        }
    }

    public FirstrunFragment createFirstRunFragment() {
        return FirstrunFragment.create();
    }

    public BrowserFragment createBrowserFragment(@Nullable String url) {
        BrowserFragment fragment = BrowserFragment.create(url);
        return fragment;
    }

    public UrlInputFragment createUrlInputFragment(@Nullable String url) {
        final UrlInputFragment fragment = UrlInputFragment.create(url);
        return fragment;
    }

    public HomeFragment createHomeFragment() {
        final HomeFragment fragment = HomeFragment.create();
        return fragment;
    }

    public void sendBrowsingTelemetry() {
        final SafeIntent intent = new SafeIntent(getIntent());
        if (intent.getBooleanExtra(EXTRA_TEXT_SELECTION, false)) {
            TelemetryWrapper.textSelectionIntentEvent();
        } else if (BrowsingSession.getInstance().isCustomTab()) {
            TelemetryWrapper.customTabsIntentEvent(BrowsingSession.getInstance().getCustomTabConfig().getOptionsList());
        } else {
            TelemetryWrapper.browseIntentEvent();
        }
    }
}
