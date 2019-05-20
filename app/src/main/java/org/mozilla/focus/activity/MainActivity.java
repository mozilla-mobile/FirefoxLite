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
import android.database.ContentObserver;
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
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
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
import org.mozilla.focus.provider.DownloadContract;
import org.mozilla.focus.repository.BookmarkRepository;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.screenshot.ScreenshotViewerActivity;
import org.mozilla.focus.tabs.tabtray.TabTray;
import org.mozilla.focus.tabs.tabtray.TabTrayFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.Browsers;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.DialogUtils;
import org.mozilla.focus.utils.FormatUtils;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ShortcutUtils;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.viewmodel.BookmarkViewModel;
import org.mozilla.focus.web.GeoPermissionCache;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.TabRestoreMonitor;
import org.mozilla.rocket.component.LaunchIntentDispatcher;
import org.mozilla.rocket.component.PrivateSessionNotificationService;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.ChromeViewModel;
import org.mozilla.rocket.chrome.ChromeViewModel.ScreenCaptureTelemetryData;
import org.mozilla.rocket.chrome.MenuViewModel;
import org.mozilla.rocket.content.ContentPortalViewState;
import org.mozilla.rocket.content.view.BottomBar;
import org.mozilla.rocket.download.DownloadIndicatorViewModel;
import org.mozilla.rocket.extension.LiveDataExtensionKt;
import org.mozilla.rocket.landing.OrientationState;
import org.mozilla.rocket.landing.PortraitComponent;
import org.mozilla.rocket.landing.PortraitStateModel;
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog;
import org.mozilla.rocket.privately.PrivateMode;
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

import kotlin.Unit;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.MENU;

public class MainActivity extends BaseActivity implements FragmentListener,
        ThemeManager.ThemeHost,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TabsSessionProvider.SessionHost, TabModelStore.AsyncQueryListener,
        TabRestoreMonitor,
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        PromotionViewContract {

    private PromotionModel promotionModel;

    // Url request from onNewIntent() need to wait till fragments are resumed.
    private String pendingUrl;

    private BottomSheetDialog menu;
    private BottomBarItemAdapter bottomBarItemAdapter;
    private MenuViewModel menuViewModel;
    private View myshotIndicator;
    private View myshotButton;
    private View snackBarContainer;
    private View nightModeButton;
    private View turboModeButton;
    private View blockImageButton;
    private View privateModeIndicator;

    private ScreenNavigator screenNavigator;

    private DialogFragment mDialogFragment;

    private BroadcastReceiver uiMessageReceiver;

    private SessionManager sessionManager;
    private boolean isTabRestoredComplete = false;
    public static final boolean ENABLE_MY_SHOT_UNREAD_DEFAULT = false;
    private static final String LOG_TAG = "MainActivity";

    private ChromeViewModel chromeViewModel;
    private BookmarkViewModel bookmarkViewModel;

    private ThemeManager themeManager;

    private boolean pendingMyShotOnBoarding;
    private Dialog myshotOnBoardingDialog;
    private DownloadIndicatorViewModel downloadIndicatorViewModel;

    private PortraitStateModel portraitStateModel = new PortraitStateModel();

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
        chromeViewModel = Inject.obtainChromeViewModel(this);
        downloadIndicatorViewModel = Inject.obtainDownloadIndicatorViewModel(this);

        asyncInitialize();

        setContentView(R.layout.activity_main);
        initViews();
        initBroadcastReceivers();

        screenNavigator = new ScreenNavigator(this);

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final String url = intent.getDataString();

                boolean openInNewTab = intent.getBooleanExtra(IntentUtils.EXTRA_OPEN_NEW_TAB,
                        false);
                this.screenNavigator.showBrowserScreen(url, openInNewTab, true);
            } else {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    this.screenNavigator.addFirstRunScreen();
                } else {
                    this.screenNavigator.popToHomeScreen(false);
                }
            }
        }
        if (NewFeatureNotice.getInstance(this).shouldShowLiteUpdate()) {
            themeManager.resetDefaultTheme();
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

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        downloadIndicatorViewModel = Inject.obtainDownloadIndicatorViewModel(this);
        observeChromeAction();

        monitorOrientationState();
    }

    private void monitorOrientationState() {
        OrientationState orientationState = new OrientationState(
                () -> ScreenNavigator.get(MainActivity.this).getNavigationState(),
                portraitStateModel);

        orientationState.observe(this, orientation -> {
            if (orientation == null) {
                return;
            }
            setRequestedOrientation(orientation);
        });
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
                        DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), snackBarContainer, LOG_TAG);
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

        final IntentFilter uiActionFilter = new IntentFilter(Constants.ACTION_NOTIFY_UI);
        uiActionFilter.addCategory(Constants.CATEGORY_FILE_OPERATION);
        uiActionFilter.addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter);
        getContentResolver().registerContentObserver(DownloadContract.Download.CONTENT_URI, true, downloadObserver);
        downloadIndicatorViewModel.updateIndicator();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver);
        getContentResolver().unregisterContentObserver(downloadObserver);

        TelemetryWrapper.stopSession();

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
        if (pendingUrl != null) {
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

    public PortraitStateModel getPortraitStateModel() {
        return portraitStateModel;
    }

    private String getSurveyUrl() {
        String currentLang = Locale.getDefault().getLanguage();
        String indonesiaLang = new Locale("id").getLanguage();

        return getString(R.string.survey_notification_url,
                currentLang.equalsIgnoreCase(indonesiaLang) ? "id" : "en");
    }

    private void setUpMenu() {
        final View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_main_menu, null);
        menu = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        menu.setContentView(sheet);
        menu.setCanceledOnTouchOutside(true);

        menu.setOnShowListener(dialog -> portraitStateModel.request(PortraitComponent.BottomMenu.INSTANCE));
        menu.setOnDismissListener(dialog -> portraitStateModel.cancelRequest(PortraitComponent.BottomMenu.INSTANCE));

        setupMenuBottomBar(menu);

        myshotIndicator = menu.findViewById(R.id.menu_my_shot_unread);
        privateModeIndicator = menu.findViewById(R.id.menu_private_mode_indicator);
        myshotButton = menu.findViewById(R.id.menu_screenshots);

        turboModeButton = menu.findViewById(R.id.menu_turbomode);
        turboModeButton.setSelected(isTurboEnabled());

        blockImageButton = menu.findViewById(R.id.menu_blockimg);
        blockImageButton.setSelected(isBlockingImages());

        nightModeButton = menu.findViewById(R.id.menu_night_mode);
        nightModeButton.setOnLongClickListener(onLongClickListener);
        nightModeButton.setSelected(isNightModeEnabled(Settings.getInstance(getApplicationContext())));
    }

    private void setupMenuBottomBar(AppCompatDialog dialog) {
        BottomBar bottomBar = dialog.findViewById(R.id.menu_bottom_bar);
        bottomBar.setOnItemClickListener((type, position) -> {
            menu.cancel();
            switch (type) {
                case BottomBarItemAdapter.TYPE_TAB_COUNTER:
                    chromeViewModel.getShowTabTray().call();
                    TelemetryWrapper.showTabTrayToolbar(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_MENU:
                    chromeViewModel.getShowMenu().call();
                    TelemetryWrapper.showMenuToolbar(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_NEW_TAB:
                    chromeViewModel.getShowNewTab().call();
                    TelemetryWrapper.clickAddTabToolbar(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_SEARCH:
                    chromeViewModel.getShowUrlInput().call();
                    TelemetryWrapper.clickToolbarSearch(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_CAPTURE:
                    chromeViewModel.getDoScreenshot().setValue(new ScreenCaptureTelemetryData(MENU, position));
                    // move Telemetry to ScreenCaptureTask doInBackground() cause we need to init category first.
                    break;
                case BottomBarItemAdapter.TYPE_PIN_SHORTCUT:
                    chromeViewModel.getPinShortcut().call();
                    TelemetryWrapper.clickAddToHome(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_BOOKMARK:
                    boolean isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK).getView().isActivated();
                    TelemetryWrapper.clickToolbarBookmark(!isActivated, MENU, position);
                    chromeViewModel.getToggleBookmark().call();
                    break;
                case BottomBarItemAdapter.TYPE_REFRESH:
                    chromeViewModel.getRefreshOrStop().call();
                    TelemetryWrapper.clickToolbarReload(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_SHARE:
                    chromeViewModel.getShare().call();
                    TelemetryWrapper.clickToolbarShare(MENU, position);
                    break;
                case BottomBarItemAdapter.TYPE_NEXT:
                    chromeViewModel.getGoNext().call();
                    TelemetryWrapper.clickToolbarForward(MENU, position);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled menu item in BrowserFragment, type: " + type);
            }
        });
        bottomBarItemAdapter = new BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.LIGHT.INSTANCE);
        menuViewModel = Inject.obtainMenuViewModel(this);
        LiveDataExtensionKt.nonNullObserve(menuViewModel.getBottomItems(), this, bottomItems -> {
            bottomBarItemAdapter.setItems(bottomItems);
            hidePinShortcutButtonIfNotSupported();
            return Unit.INSTANCE;
        });
        menuViewModel.isBottomBarEnabled().observe(this, bottomBarItemAdapter::setEnabled);

        LiveDataExtensionKt.nonNullObserve(chromeViewModel.getTabCount(), this, changedEvent -> {
            bottomBarItemAdapter.setTabCount(changedEvent.getCount(), changedEvent.getWithAnimation());
            return Unit.INSTANCE;
        });
        chromeViewModel.isRefreshing().observe(this, bottomBarItemAdapter::setRefreshing);
        chromeViewModel.getCanGoForward().observe(this, bottomBarItemAdapter::setCanGoForward);
    }

    private void hidePinShortcutButtonIfNotSupported() {
        final boolean requestPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(this);
        if (!requestPinShortcutSupported) {
            BottomBar.BottomBarItem pinShortcutItem = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_PIN_SHORTCUT);
            if (pinShortcutItem != null && pinShortcutItem.getView() != null) {
                pinShortcutItem.getView().setVisibility(View.GONE);
            }
        }
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

        turboModeButton.setSelected(isTurboEnabled());
        blockImageButton.setSelected(isBlockingImages());

        final boolean isMyShotUnreadEnabled = AppConfigWrapper.getMyshotUnreadEnabled();
        final boolean showUnread = isMyShotUnreadEnabled && Settings.getInstance(this).hasUnreadMyShot();
        final boolean privateModeActivate = PrivateMode.hasPrivateSession(this);
        final Settings settings = Settings.getInstance(getApplicationContext());

        myshotIndicator.setVisibility(showUnread ? View.VISIBLE : View.GONE);
        privateModeIndicator.setVisibility(privateModeActivate ? View.VISIBLE : View.GONE);
        if (pendingMyShotOnBoarding) {
            pendingMyShotOnBoarding = false;
            setShowNightModeSpotlight(settings, false);
            myshotButton.post(() -> myshotOnBoardingDialog = DialogUtils.showMyShotOnBoarding(
                    MainActivity.this,
                    myshotButton,
                    dialog -> dismissAllMenus(),
                    v -> {
                        final String url = SupportUtils.getSumoURLForTopic(MainActivity.this, "screenshot-telemetry");
                        this.screenNavigator.showBrowserScreen(url, true, false);
                        dismissAllMenus();
                    }));
        }

        nightModeButton.setSelected(isNightModeEnabled(settings));
        if (shouldShowNightModeSpotlight(settings)) {
            setShowNightModeSpotlight(settings, false);
            nightModeButton.post(() -> DialogUtils.showSpotlight(
                    MainActivity.this,
                    nightModeButton,
                    dialog -> {

                    }, R.string.night_mode_on_boarding_message));
        }

        final BrowserFragment browserFragment = getVisibleBrowserFragment();
        final boolean hasFocus = browserFragment != null;
        menuViewModel.onTabFocusChanged(hasFocus);
        Session current = getSessionManager().getFocusSession();
        if (current == null) {
            return;
        }
        bookmarkViewModel.getBookmarksByUrl(current.getUrl()).observe(this, bookmarks -> {
            boolean activateBookmark = bookmarks != null && bookmarks.size() > 0;
            bottomBarItemAdapter.setBookmark(activateBookmark);
        });
        bottomBarItemAdapter.setCanGoForward(chromeViewModel.getCanGoForward().getValue());

        boolean hasNewConfig = menuViewModel.refresh();
        if (hasNewConfig) {
            chromeViewModel.invalidate();
        }
    }

    private boolean isTurboEnabled() {
        return Settings.getInstance(this).shouldUseTurboMode();
    }

    private boolean isBlockingImages() {
        return Settings.getInstance(this).shouldBlockImages();
    }

    private void showListPanel(int type) {
        ListPanelDialog dialogFragment = ListPanelDialog.newInstance(type);
        dialogFragment.setCancelable(true);

        portraitStateModel.request(PortraitComponent.ListPanelDialog.INSTANCE);
        dialogFragment.setOnDismissListener(dialog ->
                portraitStateModel.cancelRequest(PortraitComponent.ListPanelDialog.INSTANCE));

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

    OnLongClickListener onLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            menu.cancel();
            switch (v.getId()) {
                case R.id.menu_night_mode:
                    final Settings settings = Settings.getInstance(getApplicationContext());

                    setNightModeEnabled(settings, true);
                    showAdjustBrightness();

                    return true;
                default:
                    throw new RuntimeException("Unknown id in menu, OnLongClickListener() is only for known ids");
            }
        }
    };

    public void onMenuItemClicked(View v) {
        final int stringResource;
        if (!v.isEnabled()) {
            return;
        }
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
                TelemetryWrapper.togglePrivateMode(true);
                break;
            case R.id.menu_night_mode:
                final Settings settings = Settings.getInstance(this);
                final boolean nightModeEnabled = !isNightModeEnabled(settings);
                v.setSelected(nightModeEnabled);

                setNightModeEnabled(settings, nightModeEnabled);
                showAdjustBrightnessIfNeeded(settings);

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
                chromeViewModel.getShowDownloadPanel().call();
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
            default:
                throw new RuntimeException("Unknown id in menu, onMenuItemClicked() is only for" +
                        " known ids");
        }
        menu.cancel();
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

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        sessionManager.destroy();
        super.onDestroy();
    }

    private void onPreferenceClicked() {
        openPreferences();
    }

    private void onExitClicked() {
        GeoPermissionCache.clear();
        if (PrivateMode.hasPrivateSession(this)) {
            final Intent intent = PrivateSessionNotificationService.buildIntent(this.getApplicationContext(), true);
            startActivity(intent);
        }
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

    private boolean shouldShowNightModeSpotlight(Settings settings) {
        return settings.showNightModeSpotlight();
    }

    private void setShowNightModeSpotlight(Settings settings, boolean enabled) {
        settings.setNightModeSpotlight(enabled);
    }

    private void showAdjustBrightness() {
        startActivity(AdjustBrightnessDialog.Intents.INSTANCE.getStartIntentFromMenu(this));
    }

    private void showAdjustBrightnessIfNeeded(Settings settings) {
        final float currentBrightness = settings.getNightModeBrightnessValue();
        if (currentBrightness == BRIGHTNESS_OVERRIDE_NONE) {
            // First time turn on
            settings.setNightModeBrightnessValue(AdjustBrightnessDialog.Constants.DEFAULT_BRIGHTNESS);
            showAdjustBrightness();

            setShowNightModeSpotlight(settings, true);
        }
    }

    private void applyNightModeBrightness(boolean enable, Settings settings, Window window) {
        final WindowManager.LayoutParams layoutParams = window.getAttributes();
        final float screenBrightness;
        if (enable) {
            screenBrightness = settings.getNightModeBrightnessValue();
        } else {
            // Disable night mode, restore the screen brightness
            screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
        }
        layoutParams.screenBrightness = screenBrightness;
        window.setAttributes(layoutParams);
    }

    private void setNightModeEnabled(Settings settings, boolean enabled) {
        chromeViewModel.onNightModeChanged(enabled);
        settings.setNightMode(enabled);
        applyNightModeBrightness(enabled, settings, getWindow());

        Fragment fragment = this.screenNavigator.getTopFragment();
        if (fragment instanceof BrowserFragment) { // null fragment will not make instanceof to be true
            ((BrowserFragment) fragment).setNightModeEnabled(enabled);
        } else if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).setNightModeEnabled(enabled);
        }
    }

    private boolean isNightModeEnabled(Settings settings) {
        return settings.isNightModeEnable();
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
        final boolean isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK).getView().isActivated();
        if (isActivated) {
            bookmarkViewModel.deleteBookmarksByUrl(currentTab.getUrl());
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_LONG).show();
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
        }
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
            setMenuButtonSelected(R.id.menu_turbomode, turboEnabled);
        } else if (this.getResources().getString(R.string.pref_key_performance_block_images).equals(key)) {
            final boolean blockingImages = isBlockingImages();
            BrowserFragment browserFragment = getBrowserFragment();
            if (browserFragment != null) {
                browserFragment.setImageBlockingEnabled(blockingImages);
            }
            setMenuButtonSelected(R.id.menu_blockimg, blockingImages);
        }
        // For turbo mode, a automatic refresh is done when we disable block image.
    }

    void setMenuButtonSelected(int buttonId, boolean selected) {
        if (menu == null) {
            return;
        }
        View button = menu.findViewById(buttonId);
        if (button == null) {
            return;
        }
        button.setSelected(selected);
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

    private void onShareClicked(final BrowserFragment browserFragment) {
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

        // if home panel has content portal displayed, hide that first.
        if (dismissContentPortal()) {
            return;
        }

        if (!this.screenNavigator.canGoBack()) {
            finish();
            return;
        }

        super.onBackPressed();
    }

    private boolean dismissContentPortal() {
        Fragment fragment = this.screenNavigator.getTopFragment();
        if (fragment instanceof HomeFragment) {
            return ((HomeFragment) fragment).hideContentPortal();
        }
        return false;
    }

    public void firstrunFinished() {
        this.screenNavigator.popToHomeScreen(false);
    }

    private void observeChromeAction() {
        chromeViewModel.getShowTabTray().observe(this, unit -> {
            TabTrayFragment tabTray = TabTray.show(getSupportFragmentManager());
            if (tabTray != null) {
                tabTray.setOnDismissListener(dialog -> portraitStateModel.cancelRequest(PortraitComponent.TabTray.INSTANCE));
                portraitStateModel.request(PortraitComponent.TabTray.INSTANCE);
            }
        });
        chromeViewModel.getShowMenu().observe(this, unit -> showMenu());
        chromeViewModel.getShowNewTab().observe(this, unit -> {
            ContentPortalViewState.reset();
            ScreenNavigator.get(this).addHomeScreen(true);
        });
        chromeViewModel.getShowUrlInput().observe(this, url -> {
            if (getSupportFragmentManager().isStateSaved()) {
                return;
            }
            this.screenNavigator.addUrlScreen(url);
        });
        chromeViewModel.getPinShortcut().observe(this, unit -> onAddToHomeClicked());
        chromeViewModel.getToggleBookmark().observe(this, unit -> onBookMarkClicked());
        chromeViewModel.getShare().observe(this, unit -> {
            BrowserFragment browserFragment = getVisibleBrowserFragment();
            if (browserFragment != null) {
                onShareClicked(browserFragment);
            }
        });
        chromeViewModel.getShowDownloadPanel().observe(this, unit -> onDownloadClicked());
    }

    @Override
    public void onNotified(@NonNull Fragment from, @NonNull TYPE type, @Nullable Object payload) {
        switch (type) {
            case OPEN_PREFERENCE:
                openPreferences();
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
            case DISMISS_URL_INPUT:
                this.screenNavigator.popUrlScreen();
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
        return UrlInputFragment.create(url, parentFragmentTag, true);
    }

    @Override
    public HomeFragment createHomeScreen() {
        return HomeFragment.create();
    }

    private void showMessage(@NonNull CharSequence msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void asyncInitialize() {
        new Thread(this::asyncCheckStorage).start();
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
    public void onQueryComplete(List<SessionManager.SessionWithState> states, String currentTabId) {
        isTabRestoredComplete = true;
        getSessionManager().restore(states, currentTabId);
        Session currentTab = getSessionManager().getFocusSession();
        if (!Settings.getInstance(this).shouldShowFirstrun() && currentTab != null && !getSupportFragmentManager().isStateSaved()) {
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
            if (s.getEngineSession() != null) {
                s.getEngineSession().saveState();
            }
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
        TelemetryWrapper.showRateApp(false);
    }

    @Override
    public void showRateAppNotification() {
        DialogUtils.showRateAppNotification(this);
        TelemetryWrapper.showRateApp(true);
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
        TelemetryWrapper.showRateApp(false);

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

    @VisibleForTesting
    @UiThread
    public void showMyShotOnBoarding() {
        pendingMyShotOnBoarding = true;
        showMenu();
    }

    private ContentObserver downloadObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            downloadIndicatorViewModel.updateIndicator();
        }
    };
}
