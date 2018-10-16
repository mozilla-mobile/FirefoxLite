/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.fragment.ListPanelDialog;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.notification.NotificationId;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.persistence.BookmarksDatabase;
import org.mozilla.focus.persistence.TabModelStore;
import org.mozilla.focus.repository.BookmarkRepository;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.screenshot.ScreenshotViewerActivity;
import org.mozilla.focus.tabs.tabtray.TabTray;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.Browsers;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.DialogUtils;
import org.mozilla.focus.utils.FormatUtils;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ShortcutUtils;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.viewmodel.BookmarkViewModel;
import org.mozilla.focus.web.GeoPermissionCache;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.AdjustBrightnessDialog;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.TabRestoreMonitor;
import org.mozilla.rocket.component.LaunchIntentDispatcher;
import org.mozilla.rocket.privately.PrivateModeActivity;
import org.mozilla.rocket.promotion.PromotionModel;
import org.mozilla.rocket.promotion.PromotionPresenter;
import org.mozilla.rocket.promotion.PromotionViewContract;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabViewProvider;
import org.mozilla.rocket.tabs.TabsSessionProvider;
import org.mozilla.rocket.theme.ThemeManager;
import org.mozilla.urlutils.UrlUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

public class MainActivity extends BaseActivity implements FragmentListener,
        ThemeManager.ThemeHost,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TabsSessionProvider.SessionHost, TabModelStore.AsyncQueryListener,
        TabRestoreMonitor,
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        PromotionViewContract {

    private PromotionModel promotionModel;

    private String pendingUrl;

    private BottomSheetDialog menu;
    private View myshotIndicator;
    private View nextButton;
    private View loadingButton;
    private View shareButton;
    private View myshotButton;
    private View bookmarkIcon;
    private View refreshIcon;
    private View stopIcon;
    private View pinShortcut;
    private View snackBarContainer;
    private View privateModeButton;

    private ScreenNavigator screenNavigator;

    private DialogFragment mDialogFragment;

    private BroadcastReceiver uiMessageReceiver;

    private SessionManager sessionManager;
    private boolean isTabRestoredComplete = false;
    public static final boolean ENABLE_MY_SHOT_UNREAD_DEFAULT = false;
    private static final String LOG_TAG = "MainActivity";

    private BookmarkViewModel bookmarkViewModel;

    private ThemeManager themeManager;

    private boolean pendingMyShotOnBoarding;
    private Dialog myshotOnBoardingDialog;

    @Override
    public ThemeManager getThemeManager() {
        return themeManager;
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();

        //  Oppo with android 5.1 call getTheme before activity onCreate invoked.
        //  So themeManager is not initialized and cause NPE
        if (themeManager != null) {
            themeManager.applyCurrentTheme(theme);
        }

        return theme;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = new ThemeManager(this);
        super.onCreate(savedInstanceState);

        asyncInitialize();

        setContentView(R.layout.activity_main);
        initViews();
        initBroadcastReceivers();

        screenNavigator = new ScreenNavigator(this);

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final String url = intent.getDataString();

                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    pendingUrl = url;
                    this.screenNavigator.addFirstRunScreen();
                } else {
                    boolean openInNewTab = intent.getBooleanExtra(IntentUtils.EXTRA_OPEN_NEW_TAB,
                            false);
                    this.screenNavigator.showBrowserScreen(url, openInNewTab, true);
                }
            } else {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    this.screenNavigator.addFirstRunScreen();
                } else {
                    this.screenNavigator.popToHomeScreen(false);
                }
            }
        }
        restoreTabsFromPersistence();
        WebViewProvider.preload(this);

        promotionModel = new PromotionModel(this, intent);

        if (Inject.getActivityNewlyCreatedFlag()) {
            Inject.setActivityNewlyCreatedFlag();

            PromotionPresenter.runPromotion(this, promotionModel);
        }

        BookmarkViewModel.Factory factory = new BookmarkViewModel.Factory(
                BookmarkRepository.getInstance(BookmarksDatabase.getInstance(this)));

        bookmarkViewModel = ViewModelProviders.of(this, factory).get(BookmarkViewModel.class);
    }

    private void initBroadcastReceivers() {
        uiMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Constants.ACTION_NOTIFY_UI:
                        final CharSequence msg = intent.getCharSequenceExtra(Constants.EXTRA_MESSAGE);
                        showMessage(msg);
                        break;
                    case Constants.ACTION_NOTIFY_RELOCATE_FINISH:
                        showOpenSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1));
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void applyLocale() {
        // re-create bottom sheet menu
        setUpMenu();
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
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        final IntentFilter uiActionFilter = new IntentFilter(Constants.ACTION_NOTIFY_UI);
        uiActionFilter.addCategory(Constants.CATEGORY_FILE_OPERATION);
        uiActionFilter.addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver);

        TelemetryWrapper.stopSession();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        saveTabsToPersistence();
    }

    @Override
    protected void onStop() {
        super.onStop();

        TelemetryWrapper.stopMainActivity();
    }

    @Override
    protected void onNewIntent(Intent unsafeIntent) {
        final SafeIntent intent = new SafeIntent(unsafeIntent);
        if (promotionModel != null) {
            promotionModel.parseIntent(intent);
            if (PromotionPresenter.runPromotionFromIntent(this, promotionModel)) {
                // Don't run other promotion or other action if we already displayed above promotion
                return;
            }
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // We can't update our fragment right now because we need to wait until the activity is
            // resumed. So just remember this URL and load it in onResumeFragments().
            pendingUrl = intent.getDataString();
            // We don't want to see any menu is visible when processing open url request from Intent.ACTION_VIEW
            dismissAllMenus();
            TabTray.dismiss(getSupportFragmentManager());
        }

        // We do not care about the previous intent anymore. But let's remember this one.
        setIntent(unsafeIntent);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (pendingUrl != null && !Settings.getInstance(this).shouldShowFirstrun()) {
            // We have received an URL in onNewIntent(). Let's load it now.
            // Unless we're trying to show the firstrun screen, in which case we leave it pending until
            // firstrun is dismissed.
            final SafeIntent intent = new SafeIntent(getIntent());
            boolean openInNewTab = intent.getBooleanExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true);
            this.screenNavigator.showBrowserScreen(pendingUrl, openInNewTab, true);
            pendingUrl = null;
        }
    }

    private void initViews() {
        int visibility = getWindow().getDecorView().getSystemUiVisibility();
        // do not overwrite existing value
        visibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(visibility);

        snackBarContainer = findViewById(R.id.container);
        setUpMenu();
    }


    public void postSurveyNotification() {
        Intent intent = IntentUtils.createInternalOpenUrlIntent(this,
                getSurveyUrl(), true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        final NotificationCompat.Builder builder = NotificationUtil.importantBuilder(this)
                .setContentTitle(getString(R.string.survey_notification_title, "\uD83D\uDE4C"))
                .setContentText(getString(R.string.survey_notification_description))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        getString(R.string.survey_notification_description)))
                .setContentIntent(pendingIntent);

        NotificationUtil.sendNotification(this, NotificationId.SURVEY_ON_3RD_LAUNCH, builder);
    }

    private String getSurveyUrl() {
        String currentLang = Locale.getDefault().getLanguage();
        String indonesiaLang = new Locale("id").getLanguage();

        return getString(R.string.survey_notification_url,
                currentLang.equalsIgnoreCase(indonesiaLang) ? "id" : "en");
    }

    private void setUpMenu() {
        final View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_main_menu, (ViewGroup) null);
        menu = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        menu.setContentView(sheet);
        menu.setCanceledOnTouchOutside(true);
        myshotIndicator = menu.findViewById(R.id.menu_my_shot_unread);
        nextButton = menu.findViewById(R.id.action_next);
        loadingButton = menu.findViewById(R.id.action_loading);
        privateModeButton = menu.findViewById(R.id.btn_private_browsing);
        shareButton = menu.findViewById(R.id.action_share);
        bookmarkIcon = menu.findViewById(R.id.action_bookmark);
        refreshIcon = menu.findViewById(R.id.action_refresh);
        stopIcon = menu.findViewById(R.id.action_stop);
        pinShortcut = menu.findViewById(R.id.action_pin_shortcut);
        myshotButton = menu.findViewById(R.id.menu_screenshots);
        final boolean requestPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(this);
        if (!requestPinShortcutSupported) {
            pinShortcut.setVisibility(View.GONE);
        }
        menu.findViewById(R.id.menu_turbomode).setSelected(isTurboEnabled());
        menu.findViewById(R.id.menu_blockimg).setSelected(isBlockingImages());
        menu.findViewById(R.id.menu_night_mode).setSelected(isNightModeEnabled());

    }

    public BrowserFragment getVisibleBrowserFragment() {
        return screenNavigator.isBrowserInForeground() ? getBrowserFragment() : null;
    }

    private void openUrl(final boolean withNewTab, final Object payload) {
        final String url = (payload != null) ? payload.toString() : null;
        ScreenNavigator.get(this).showBrowserScreen(url, withNewTab, false);
    }

    private void showMenu() {
        updateMenu();
        menu.show();
    }

    private void updateMenu() {
        final boolean isMyShotUnreadEnabled = AppConfigWrapper.getMyshotUnreadEnabled(this);
        final boolean showUnread = isMyShotUnreadEnabled && Settings.getInstance(this).hasUnreadMyShot();
        myshotIndicator.setVisibility(showUnread ? View.VISIBLE : View.GONE);
        if (pendingMyShotOnBoarding) {
            myshotButton.post(() -> myshotOnBoardingDialog = DialogUtils.showMyShotOnBoarding(
                    MainActivity.this,
                    myshotButton,
                    dialog -> dismissAllMenus(),
                    v -> {
                        final String url = SupportUtils.getSumoURLForTopic(MainActivity.this, "screenshot-telemetry");
                        this.screenNavigator.showBrowserScreen(url, true, false);
                        dismissAllMenus();
                    }));
            pendingMyShotOnBoarding = false;
        }
        final BrowserFragment browserFragment = getVisibleBrowserFragment();
        final boolean canGoForward = browserFragment != null && browserFragment.canGoForward();
        setEnable(nextButton, canGoForward);
        setLoadingButton(browserFragment);
        setEnable(bookmarkIcon, browserFragment != null);
        setEnable(shareButton, browserFragment != null);
        setEnable(pinShortcut, browserFragment != null);
        Session current = getSessionManager().getFocusSession();
        if (current == null) {
            return;
        }

        bookmarkViewModel.getBookmarksByUrl(current.getUrl()).observe(this, bookmarks -> {
            boolean activateBookmark = bookmarks != null && bookmarks.size() > 0;
            bookmarkIcon.setActivated(activateBookmark);
        });
    }

    private boolean isTurboEnabled() {
        return Settings.getInstance(this).shouldUseTurboMode();
    }

    private boolean isBlockingImages() {
        return Settings.getInstance(this).shouldBlockImages();
    }

    private void showListPanel(int type) {
        DialogFragment dialogFragment = ListPanelDialog.newInstance(type);
        dialogFragment.setCancelable(true);
        dialogFragment.show(getSupportFragmentManager(), "");
        mDialogFragment = dialogFragment;
    }

    private void dismissAllMenus() {
        if (menu != null) {
            menu.dismiss();
        }
        BrowserFragment browserFragment = getVisibleBrowserFragment();
        if (browserFragment != null) {
            browserFragment.dismissWebContextMenu();
            browserFragment.dismissGeoDialog();
        }
        if (mDialogFragment != null) {
            mDialogFragment.dismissAllowingStateLoss();
        }
        if (myshotOnBoardingDialog != null) {
            myshotOnBoardingDialog.dismiss();
            myshotOnBoardingDialog = null;
        }
    }

    public void onMenuItemClicked(View v) {
        final int stringResource;
        if (!v.isEnabled()) {
            return;
        }
        menu.cancel();
        switch (v.getId()) {
            case R.id.menu_blockimg:
                //  Toggle
                final boolean blockingImages = !isBlockingImages();
                Settings.getInstance(this).setBlockImages(blockingImages);

                v.setSelected(blockingImages);
                stringResource = blockingImages ? R.string.message_enable_block_image : R.string.message_disable_block_image;
                Toast.makeText(this, stringResource, Toast.LENGTH_SHORT).show();

                TelemetryWrapper.menuBlockImageChangeTo(blockingImages);
                break;
            case R.id.menu_turbomode:
                //  Toggle
                final boolean turboEnabled = !isTurboEnabled();
                Settings.getInstance(this).setTurboMode(turboEnabled);

                v.setSelected(turboEnabled);
                stringResource = turboEnabled ? R.string.message_enable_turbo_mode : R.string.message_disable_turbo_mode;
                Toast.makeText(this, stringResource, Toast.LENGTH_SHORT).show();

                TelemetryWrapper.menuTurboChangeTo(turboEnabled);
                break;
            case R.id.btn_private_browsing:
                Intent intent = new Intent(this, PrivateModeActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.tab_transition_fade_in, R.anim.tab_transition_fade_out);
                break;
            case R.id.menu_night_mode:
                final boolean nightModeEnabled = !isNightModeEnabled();
                v.setSelected(nightModeEnabled);
                onNightModeClicked(nightModeEnabled);
                TelemetryWrapper.menuNightModeChangeTo(nightModeEnabled);
                break;
            case R.id.menu_find_in_page:
                onFindInPageClicked();
                break;
            case R.id.menu_delete:
                onDeleteClicked();
                TelemetryWrapper.clickMenuClearCache();
                break;
            case R.id.menu_download:
                onDownloadClicked();
                TelemetryWrapper.clickMenuDownload();
                break;
            case R.id.menu_history:
                onHistoryClicked();
                TelemetryWrapper.clickMenuHistory();
                break;
            case R.id.menu_screenshots:
                onScreenshotsClicked();
                TelemetryWrapper.clickMenuCapture();
                break;
            case R.id.menu_preferences:
                driveDefaultBrowser();
                onPreferenceClicked();
                TelemetryWrapper.clickMenuSettings();
                break;
            case R.id.menu_exit:
                onExitClicked();
                TelemetryWrapper.clickMenuExit();
                break;
            case R.id.menu_bookmark:
                onBookmarksClicked();
                TelemetryWrapper.clickMenuBookmark();
                break;
            case R.id.action_next:
            case R.id.action_loading:
            case R.id.action_bookmark:
            case R.id.action_share:
            case R.id.action_pin_shortcut:
                onMenuBrowsingItemClicked(v);
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuItemClicked() is only for" +
                        " known ids");
        }
    }

    private void driveDefaultBrowser() {
        final Settings settings = Settings.getInstance(this);
        if (settings.isDefaultBrowserSettingDidShow()) {
            // We don't need to accumulate the count after we've displayed the default browser promotion
            return;
        } else {
            settings.addMenuPreferenceClickCount();
        }

        final int count = settings.getMenuPreferenceClickCount();
        final int threshold = AppConfigWrapper.getDriveDefaultBrowserFromMenuSettingThreshold();
        // even if user above threshold and not set-as-default-browser, still don't show notification.
        if (count == threshold && !Browsers.isDefaultBrowser(this)) {
            DialogUtils.showDefaultSettingNotification(this);
            TelemetryWrapper.showDefaultSettingNotification();
        }
    }

    private void setEnable(View v, boolean enable) {
        v.setEnabled(enable);
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                setEnable(((ViewGroup) v).getChildAt(i), enable);
            }
        }
    }

    private void setLoadingButton(BrowserFragment fragment) {
        if (fragment == null) {
            setEnable(loadingButton, false);
            refreshIcon.setVisibility(View.VISIBLE);
            stopIcon.setVisibility(View.GONE);
            loadingButton.setTag(false);
        } else {
            setEnable(loadingButton, true);
            boolean isLoading = fragment.isLoading();
            refreshIcon.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            stopIcon.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loadingButton.setTag(isLoading);
        }
    }

    public void onMenuBrowsingItemClicked(View v) {
        final BrowserFragment browserFragment = getVisibleBrowserFragment();
        if (browserFragment == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.action_next:
                onNextClicked(browserFragment);
                TelemetryWrapper.clickToolbarForward();
                break;
            case R.id.action_loading:
                if ((boolean) v.getTag()) {
                    onStopClicked(browserFragment);
                } else {
                    onRefreshClicked(browserFragment);
                }
                TelemetryWrapper.clickToolbarReload();
                break;
            case R.id.action_bookmark:
                onBookMarkClicked();
                break;
            case R.id.action_share:
                onShraeClicked(browserFragment);
                TelemetryWrapper.clickToolbarShare();
                break;
            case R.id.action_pin_shortcut:
                onAddToHomeClicked();
                TelemetryWrapper.clickAddToHome();
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuBrowsingItemClicked() is" +
                        " only for known ids");
        }
    }

    @Override
    public void onDestroy() {
        sessionManager.destroy();
        super.onDestroy();
    }

    private void onPreferenceClicked() {
        openPreferences();
    }

    private void onExitClicked() {
        GeoPermissionCache.clear();
        finish();
    }


    private void onBookmarksClicked() {
        showListPanel(ListPanelDialog.TYPE_BOOKMARKS);
    }

    private void onDownloadClicked() {
        showListPanel(ListPanelDialog.TYPE_DOWNLOADS);
    }

    private void onHistoryClicked() {
        showListPanel(ListPanelDialog.TYPE_HISTORY);
    }

    private void onScreenshotsClicked() {
        Settings.getInstance(this).setHasUnreadMyShot(false);
        showListPanel(ListPanelDialog.TYPE_SCREENSHOTS);
    }

    private void onFindInPageClicked() {
        final BrowserFragment frg = getVisibleBrowserFragment();
        if (frg != null) {
            frg.showFindInPage();
        }
    }

    private void onDeleteClicked() {
        final long diff = FileUtils.clearCache(this);
        final int stringId = (diff < 0) ? R.string.message_clear_cache_fail : R.string.message_cleared_cached;
        final String msg = getString(stringId, FormatUtils.getReadableStringFromFileSize(diff));
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void onNightModeClicked(final boolean enable) {
        final Settings settings = Settings.getInstance(this);
        settings.setNightMode(enable);
        final float currentBrightness = settings.getNightModeBrightnessValue();
        final WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (enable) {
            if (currentBrightness == BRIGHTNESS_OVERRIDE_NONE) {
                // First time turn on
                settings.setNightModeBrightnessValue(AdjustBrightnessDialog.Constants.DEFAULT_BRIGHTNESS);
                startActivity(AdjustBrightnessDialog.Intents.INSTANCE.getStartIntentFromMenu(this));
            } else {
                layoutParams.screenBrightness = currentBrightness;
                getWindow().setAttributes(layoutParams);
            }
        } else {
            // Disable night mode, restore the screen brightness
            layoutParams.screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(layoutParams);
        }

        Fragment fragment = this.screenNavigator.getTopFragment();
        if (fragment instanceof BrowserFragment) { // null fragment will not make instanceof to be true
            ((BrowserFragment) fragment).setNightModeEnabled(enable);
        } else if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).setNightModeEnabled(enable);
        }
    }

    private boolean isNightModeEnabled() {
        return Settings.getInstance(this).isNightModeEnable();
    }

    @VisibleForTesting
    public BrowserFragment getBrowserFragment() {
        return (BrowserFragment) getSupportFragmentManager().findFragmentById(R.id.browser);
    }

    private void onBookMarkClicked() {
        Session currentTab = getSessionManager().getFocusSession();
        if (currentTab == null) {
            return;
        }
        final boolean isActivated = bookmarkIcon.isActivated();
        TelemetryWrapper.clickToolbarBookmark(!isActivated);
        if (isActivated) {
            bookmarkViewModel.deleteBookmarksByUrl(currentTab.getUrl());
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_LONG).show();
            bookmarkIcon.setActivated(false);
        } else {
            if (TextUtils.isEmpty(currentTab.getUrl())) {
                //TODO: Edge case - should add a hint for failing to add the bookmark
                return;
            }
            final String originalTitle = currentTab.getTitle();
            final String title = TextUtils.isEmpty(originalTitle) ? UrlUtils.stripCommonSubdomains(UrlUtils.stripHttp(currentTab.getUrl())) : originalTitle;
            final String itemId = bookmarkViewModel.addBookmark(title, currentTab.getUrl());
            final Snackbar snackbar = Snackbar.make(snackBarContainer, R.string.bookmark_saved, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.bookmark_saved_edit, view -> startActivity(new Intent(this, EditBookmarkActivity.class).putExtra(EditBookmarkActivityKt.ITEM_UUID_KEY, itemId)));
            snackbar.show();
            bookmarkIcon.setActivated(true);
        }
    }

    private void onNextClicked(final BrowserFragment browserFragment) {
        browserFragment.goForward();
    }

    private void onRefreshClicked(final BrowserFragment browserFragment) {
        browserFragment.reload();
    }

    private void onStopClicked(final BrowserFragment browserFragment) {
        browserFragment.stop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Only refresh when disabling turbo mode
        if (this.getResources().getString(R.string.pref_key_turbo_mode).equals(key)) {
            final boolean turboEnabled = isTurboEnabled();
            BrowserFragment browserFragment = getBrowserFragment();
            if (browserFragment != null) {
                browserFragment.setContentBlockingEnabled(turboEnabled);
            }
            menu.findViewById(R.id.menu_turbomode).setSelected(turboEnabled);
        } else if (this.getResources().getString(R.string.pref_key_performance_block_images).equals(key)) {
            final boolean blockingImages = isBlockingImages();
            BrowserFragment browserFragment = getBrowserFragment();
            if (browserFragment != null) {
                browserFragment.setImageBlockingEnabled(blockingImages);
            }
            menu.findViewById(R.id.menu_blockimg).setSelected(blockingImages);
        }
        // For turbo mode, a automatic refresh is done when we disable block image.
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ScreenshotViewerActivity.REQ_CODE_VIEW_SCREENSHOT) {
            if (resultCode == ScreenshotViewerActivity.RESULT_NOTIFY_SCREENSHOT_IS_DELETED) {
                Toast.makeText(this, R.string.message_deleted_screenshot, Toast.LENGTH_SHORT).show();
                if (mDialogFragment != null) {
                    Fragment fragment = mDialogFragment.getChildFragmentManager().findFragmentById(R.id.main_content);
                    if (fragment instanceof ScreenshotGridFragment && data != null) {
                        long id = data.getLongExtra(ScreenshotViewerActivity.EXTRA_SCREENSHOT_ITEM_ID, -1);
                        ((ScreenshotGridFragment) fragment).notifyItemDelete(id);
                    }
                }
            } else if (resultCode == ScreenshotViewerActivity.RESULT_OPEN_URL) {
                if (data != null) {
                    String url = data.getStringExtra(ScreenshotViewerActivity.EXTRA_URL);
                    if (mDialogFragment != null) {
                        mDialogFragment.dismissAllowingStateLoss();
                    }
                    screenNavigator.showBrowserScreen(url, true, false);
                }
            }
        }
    }

    private void onShraeClicked(final BrowserFragment browserFragment) {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, browserFragment.getUrl());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)));
    }

    private void onAddToHomeClicked() {
        final Session focusTab = getSessionManager().getFocusSession();
        if (focusTab == null) {
            return;
        }
        final String url = focusTab.getUrl();
        // If we pin an invalid url as shortcut, the app will not function properly.
        // TODO: only enable the bottom menu item if the page is valid and loaded.
        if (!SupportUtils.isUrl(url)) {
            return;
        }
        final Bitmap bitmap = focusTab.getFavicon();
        final Intent shortcut = new Intent(Intent.ACTION_VIEW);
        // Use activity-alias name here so we can start whoever want to control launching behavior
        // Besides, RocketLauncherActivity not exported so using the alias-name is required.
        shortcut.setClassName(this, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
        shortcut.setData(Uri.parse(url));
        shortcut.putExtra(LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_HOME_SCREEN_SHORTCUT.getValue(), true);

        ShortcutUtils.requestPinShortcut(this, shortcut, focusTab.getTitle(), url, bitmap);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().isStateSaved()) {
            return;
        }

        ScreenNavigator.BrowserScreen browserScreen = screenNavigator.getVisibleBrowserScreen();
        if (browserScreen != null && browserScreen.onBackPressed()) {
            return;
        }

        if (!this.screenNavigator.canGoBack()) {
            finish();
            return;
        }

        super.onBackPressed();
    }

    public void firstrunFinished() {
        if (pendingUrl != null) {
            // We have received an URL in onNewIntent(). Let's load it now.
            this.screenNavigator.showBrowserScreen(pendingUrl, true, true);
            pendingUrl = null;
        } else {
            this.screenNavigator.popToHomeScreen(false);
        }
    }

    @Override
    public void onNotified(@NonNull Fragment from, @NonNull TYPE type, @Nullable Object payload) {
        switch (type) {
            case OPEN_PREFERENCE:
                openPreferences();
                break;
            case SHOW_MENU:
                this.showMenu();
                break;
            case UPDATE_MENU:
                this.updateMenu();
                break;
            case OPEN_URL_IN_CURRENT_TAB:
                openUrl(false, payload);
                break;
            case OPEN_URL_IN_NEW_TAB:
                openUrl(true, payload);
                break;
            case SHOW_URL_INPUT:
                if (getSupportFragmentManager().isStateSaved()) {
                    return;
                }
                final String url = (payload != null) ? payload.toString() : null;
                this.screenNavigator.addUrlScreen(url);
                break;
            case DISMISS_URL_INPUT:
                this.screenNavigator.popUrlScreen();
                break;
            case SHOW_TAB_TRAY:
                TabTray.show(getSupportFragmentManager());
                break;
            case REFRESH_TOP_SITE:
                Fragment fragment = this.screenNavigator.getTopFragment();
                if (fragment instanceof HomeFragment) {
                    ((HomeFragment) fragment).updateTopSitesData();
                }
                break;
            case SHOW_MY_SHOT_ON_BOARDING:
                showMyShotOnBoarding();
                break;
            default:
                break;
        }
    }

    @Override
    public ScreenNavigator getScreenNavigator() {
        return screenNavigator;
    }

    @Override
    public FirstrunFragment createFirstRunScreen() {
        return FirstrunFragment.create();
    }

    @Override
    public BrowserFragment getBrowserScreen() {
        return (BrowserFragment) getSupportFragmentManager().findFragmentById(R.id.browser);
    }

    @Override
    public UrlInputFragment createUrlInputScreen(@Nullable String url, String parentFragmentTag) {
        final UrlInputFragment fragment = UrlInputFragment.create(url, parentFragmentTag, true);
        return fragment;
    }

    @Override
    public HomeFragment createHomeScreen() {
        final HomeFragment fragment = HomeFragment.create();
        return fragment;
    }

    @Override
    public void onBrowserScreenRaised() {
        final SafeIntent intent = new SafeIntent(getIntent());
        if (intent.getBooleanExtra(LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_TEXT_SELECTION.getValue(), false)) {
            TelemetryWrapper.textSelectionIntentEvent();
        } else {
            TelemetryWrapper.browseIntentEvent();
        }
    }

    private void showMessage(@NonNull CharSequence msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void asyncInitialize() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                asyncCheckStorage();
            }
        })).start();
    }

    /**
     * To check existence of removable storage, and write result to preference
     */
    private void asyncCheckStorage() {
        boolean exist;
        try {
            final File dir = StorageUtils.getTargetDirOnRemovableStorageForDownloads(this, "*/*");
            exist = (dir != null);
        } catch (NoRemovableStorageException e) {
            exist = false;
        }

        Settings.getInstance(this).setRemovableStorageStateOnCreate(exist);
    }

    private void showOpenSnackBar(Long rowId) {
        DownloadInfoManager.getInstance().queryByRowId(rowId, downloadInfoList -> {
            final boolean existInLocalDB = downloadInfoList.size() > 0;
            if (!existInLocalDB) {
                logOrCrash("Download Completed with unknown local row id");
                return;
            }
            final DownloadInfo downloadInfo = (DownloadInfo) downloadInfoList.get(0);
            final boolean existInDownloadManager = downloadInfo.existInDownloadManager();
            if (!existInDownloadManager) {
                logOrCrash("Download Completed with unknown DownloadManager id");
            }
            String completedStr = getString(R.string.download_completed, downloadInfo.getFileName());
            final Snackbar snackbar = Snackbar.make(snackBarContainer, completedStr, Snackbar.LENGTH_LONG);
            // Set the open action only if we can.
            if (existInDownloadManager) {
                snackbar.setAction(R.string.open, view -> IntentUtils.intentOpenFile(MainActivity.this, downloadInfo.getFileUri(), downloadInfo.getMimeType()));
            }
            snackbar.show();
        });
    }

    private void logOrCrash(String message) {
        if (AppConstants.isReleaseBuild()) {
            Log.e(LOG_TAG, message);
        } else {
            throw new IllegalStateException(message);
        }
    }

    @Override
    public SessionManager getSessionManager() {
        // TODO: Find a proper place to allocate and init SessionManager
        if (sessionManager == null) {
            final TabViewProvider provider = new MainTabViewProvider(this);
            sessionManager = new SessionManager(provider);
        }
        return sessionManager;
    }

    @Override
    public boolean isTabRestoredComplete() {
        return isTabRestoredComplete;
    }

    @Override
    public void onQueryComplete(List<Session> session, String currentTabId) {
        isTabRestoredComplete = true;
        getSessionManager().restoreTabs(session, currentTabId);
        Session currentTab = getSessionManager().getFocusSession();
        if (currentTab != null && !getSupportFragmentManager().isStateSaved()) {
            screenNavigator.restoreBrowserScreen(currentTab.getId());
        }
    }

    private void restoreTabsFromPersistence() {
        isTabRestoredComplete = false;
        TabModelStore.getInstance(this).getSavedTabs(this, this);
    }

    private void saveTabsToPersistence() {
        if (!isTabRestoredComplete) {
            return;
        }

        List<Session> sessions = getSessionManager().getTabs();
        for (Session s : sessions) {
            s.syncFromView();
        }

        final String currentTabId = (getSessionManager().getFocusSession() != null)
                ? getSessionManager().getFocusSession().getId()
                : null;

        TabModelStore.getInstance(this).saveTabs(this, sessions, currentTabId, null);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Settings.EventHistory history = Settings.getInstance(this).getEventHistory();
        history.add(Settings.Event.PostSurveyNotification);

    }

    @Override
    public void showRateAppDialog() {
        DialogUtils.showRateAppDialog(this);
        TelemetryWrapper.showFeedbackDialog();
    }

    @Override
    public void showRateAppNotification() {
        DialogUtils.showRateAppNotification(this);
        TelemetryWrapper.showRateAppNotification();
    }

    @Override
    public void showShareAppDialog() {
        DialogUtils.showShareAppDialog(this);
        TelemetryWrapper.showPromoteShareDialog();
    }

    @Override
    public void showPrivacyPolicyUpdateNotification() {
        DialogUtils.showPrivacyPolicyUpdateNotification(this);

    }

    @Override
    public void showRateAppDialogFromIntent() {

        DialogUtils.showRateAppDialog(this);
        TelemetryWrapper.showFeedbackDialog();

        NotificationManagerCompat.from(this).cancel(NotificationId.LOVE_FIREFOX);

        // Reset extra after dialog displayed.
        if (getIntent().getExtras() != null) {
            getIntent().getExtras().putBoolean(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false);
        }
    }

    // a TabViewProvider and it should only be used in this activity
    private static class MainTabViewProvider extends TabViewProvider {
        private Activity activity;

        MainTabViewProvider(@NonNull final Activity activity) {
            this.activity = activity;
        }

        @Override
        public TabView create() {
            // FIXME: we should avoid casting here.
            // TabView and View is totally different, we know WebViewProvider returns a TabView for now,
            // but there is no promise about this.
            return (TabView) WebViewProvider.create(this.activity, null);
        }
    }

    @UiThread
    private void showMyShotOnBoarding() {
        pendingMyShotOnBoarding = true;
        showMenu();
    }

}
