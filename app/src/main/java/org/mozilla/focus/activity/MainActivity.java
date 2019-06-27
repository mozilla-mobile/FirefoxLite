/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

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
import org.mozilla.focus.persistence.TabModelStore;
import org.mozilla.focus.provider.DownloadContract;
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
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ShortcutUtils;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.web.GeoPermissionCache;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.rocket.appupdate.InAppUpdateManager;
import org.mozilla.rocket.appupdate.InAppUpdateModelRepository;
import org.mozilla.rocket.chrome.ChromeViewModel;
import org.mozilla.rocket.chrome.ChromeViewModel.OpenUrlAction;
import org.mozilla.rocket.component.LaunchIntentDispatcher;
import org.mozilla.rocket.component.PrivateSessionNotificationService;
import org.mozilla.rocket.content.ContentPortalViewState;
import org.mozilla.rocket.download.DownloadIndicatorViewModel;
import org.mozilla.rocket.landing.DialogQueue;
import org.mozilla.rocket.appupdate.InAppUpdateViewDelegate;
import org.mozilla.rocket.landing.OrientationState;
import org.mozilla.rocket.landing.PortraitComponent;
import org.mozilla.rocket.landing.PortraitStateModel;
import org.mozilla.rocket.menu.MenuDialog;
import org.mozilla.rocket.privately.PrivateMode;
import org.mozilla.rocket.promotion.PromotionModel;
import org.mozilla.rocket.promotion.PromotionPresenter;
import org.mozilla.rocket.promotion.PromotionViewContract;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabViewProvider;
import org.mozilla.rocket.tabs.TabsSessionProvider;
import org.mozilla.rocket.theme.ThemeManager;
import org.mozilla.rocket.widget.PromotionDialog;
import org.mozilla.rocket.widget.PromotionDialogExt;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

public class MainActivity extends BaseActivity implements ThemeManager.ThemeHost,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TabsSessionProvider.SessionHost, TabModelStore.AsyncQueryListener,
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        PromotionViewContract {

    public static final int REQUEST_CODE_IN_APP_UPDATE = 1024;
    public static final String ACTION_INSTALL_IN_APP_UPDATE = "install_in_app_update";

    private PromotionModel promotionModel;

    private MenuDialog menu;
    private View snackBarContainer;

    private ScreenNavigator screenNavigator;

    private DialogFragment mDialogFragment;

    private BroadcastReceiver uiMessageReceiver;

    private SessionManager sessionManager;
    public static final boolean ENABLE_MY_SHOT_UNREAD_DEFAULT = false;
    private static final String LOG_TAG = "MainActivity";

    private ChromeViewModel chromeViewModel;

    private ThemeManager themeManager;

    private Dialog myshotOnBoardingDialog;
    private DownloadIndicatorViewModel downloadIndicatorViewModel;

    private PortraitStateModel portraitStateModel = new PortraitStateModel();
    private DialogQueue dialogQueue = new DialogQueue();

    private InAppUpdateManager appUpdateManager;

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
        screenNavigator = new ScreenNavigator(this);
        chromeViewModel = Inject.obtainChromeViewModel(this);
        screenNavigator.getNavigationState().observe(this, state ->
                chromeViewModel.getNavigationState().setValue(state));
        downloadIndicatorViewModel = Inject.obtainDownloadIndicatorViewModel(this);

        asyncInitialize();

        setContentView(R.layout.activity_main);
        initViews();
        initBroadcastReceivers();

        appUpdateManager = new InAppUpdateManager(
                new InAppUpdateViewDelegate(this, dialogQueue, snackBarContainer),
                new InAppUpdateModelRepository(Settings.getInstance(this)));

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            boolean handledExternalLink = handleExternalLink(intent);
            if (!handledExternalLink) {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    screenNavigator.addFirstRunScreen();
                } else {
                    screenNavigator.popToHomeScreen(false);
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


        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        observeChromeAction();

        monitorOrientationState();

        installUpdateIfNeeded();
    }

    private void monitorOrientationState() {
        OrientationState orientationState = new OrientationState(
                () -> screenNavigator.getNavigationState(),
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
        boolean isFirstRunShowing = Settings.getInstance(this).shouldShowFirstrun();
        boolean isInAppUpdateInstallIntent = isInAppUpdateInstallIntent(getIntent());
        if (!isFirstRunShowing && !isInAppUpdateInstallIntent) {
            appUpdateManager.update(this, AppConfigWrapper.getInAppUpdateConfig());
        }
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
        boolean handledExternalLink = handleExternalLink(intent);
        if (handledExternalLink) {
            // We don't want to see any menu is visible when processing open url request from Intent.ACTION_VIEW
            dismissAllMenus();
            TabTray.dismiss(getSupportFragmentManager());
        }

        // We do not care about the previous intent anymore. But let's remember this one.
        setIntent(unsafeIntent);

        installUpdateIfNeeded();
    }

    private boolean handleExternalLink(SafeIntent intent) {
        boolean handled = false;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String url = intent.getDataString();
            String nonNullUrl = url != null ? url : "";
            boolean openInNewTab = intent.getBooleanExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true);
            chromeViewModel.getOpenUrl().setValue(new OpenUrlAction(nonNullUrl, openInNewTab, true));
            handled = true;
        }

        return handled;
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
        menu = new MenuDialog(this, R.style.BottomSheetTheme);
        menu.setCanceledOnTouchOutside(true);
        menu.setOnShowListener(dialog -> portraitStateModel.request(PortraitComponent.BottomMenu.INSTANCE));
        menu.setOnDismissListener(dialog -> portraitStateModel.cancelRequest(PortraitComponent.BottomMenu.INSTANCE));
    }

    public BrowserFragment getVisibleBrowserFragment() {
        return screenNavigator.isBrowserInForeground() ? getBrowserFragment() : null;
    }

    private void showMenu() {
        menu.show();
    }

    private boolean isTurboEnabled() {
        return Settings.getInstance(this).shouldUseTurboMode();
    }

    private boolean isBlockingImages() {
        return Settings.getInstance(this).shouldBlockImages();
    }

    public void showListPanel(int type) {
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
        if (sessionManager != null) {
            sessionManager.destroy();
        }
        super.onDestroy();
    }

    private void onExitClicked() {
        GeoPermissionCache.clear();
        if (PrivateMode.getInstance(this).hasPrivateSession()) {
            final Intent intent = PrivateSessionNotificationService.buildIntent(this.getApplicationContext(), true);
            startActivity(intent);
        }
        finish();
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

    private void onNightModeEnabled(Settings settings, boolean enabled) {
        applyNightModeBrightness(enabled, settings, getWindow());

        Fragment fragment = screenNavigator.getTopFragment();
        if (fragment instanceof BrowserFragment) { // null fragment will not make instanceof to be true
            ((BrowserFragment) fragment).setNightModeEnabled(enabled);
        } else if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).setNightModeEnabled(enabled);
        }
    }

    @VisibleForTesting
    public BrowserFragment getBrowserFragment() {
        return (BrowserFragment) getSupportFragmentManager().findFragmentById(R.id.browser);
    }

    private void onBookMarkClicked() {
        Boolean isActivated = chromeViewModel.isCurrentUrlBookmarked().getValue();
        if (isActivated != null && isActivated) {
            chromeViewModel.deleteBookmark();
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_LONG).show();
        } else {
            final String itemId = chromeViewModel.addBookmark();
            if (itemId != null) {
                final Snackbar snackbar = Snackbar.make(snackBarContainer, R.string.bookmark_saved, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.bookmark_saved_edit, view -> startActivity(new Intent(this, EditBookmarkActivity.class).putExtra(EditBookmarkActivityKt.ITEM_UUID_KEY, itemId)));
                snackbar.show();
            }
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

        } else if (requestCode == REQUEST_CODE_IN_APP_UPDATE) {
            if (resultCode == Activity.RESULT_OK) {
                appUpdateManager.onInAppUpdateGranted();
            } else {
                appUpdateManager.onInAppUpdateDenied();
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

        if (!screenNavigator.canGoBack()) {
            finish();
            return;
        }

        super.onBackPressed();
    }

    private boolean dismissContentPortal() {
        Fragment fragment = screenNavigator.getTopFragment();
        if (fragment instanceof HomeFragment) {
            return ((HomeFragment) fragment).hideContentPortal();
        }
        return false;
    }

    public void firstrunFinished() {
        screenNavigator.popToHomeScreen(false);
    }

    private void observeChromeAction() {
        chromeViewModel.getShowToast().observe(this, toastMessage -> {
            if (toastMessage != null) {
                Toast.makeText(this, getString(toastMessage.getStringResId(), (Object[]) toastMessage.getArgs()), toastMessage.getDuration()).show();
            }
        });
        chromeViewModel.getOpenUrl().observe(this, action -> {
            if (action != null) {
                screenNavigator.showBrowserScreen(action.getUrl(), action.getWithNewTab(), action.isFromExternal());
            }
        });
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
            screenNavigator.addHomeScreen(true);
        });
        chromeViewModel.getShowUrlInput().observe(this, url -> {
            if (getSupportFragmentManager().isStateSaved()) {
                return;
            }
            screenNavigator.addUrlScreen(url);
        });
        chromeViewModel.getDismissUrlInput().observe(this, unit -> screenNavigator.popUrlScreen());
        chromeViewModel.getPinShortcut().observe(this, unit -> onAddToHomeClicked());
        chromeViewModel.getToggleBookmark().observe(this, unit -> onBookMarkClicked());
        chromeViewModel.getShare().observe(this, unit -> {
            BrowserFragment browserFragment = getVisibleBrowserFragment();
            if (browserFragment != null) {
                onShareClicked(browserFragment);
            }
        });
        chromeViewModel.getShowDownloadPanel().observe(this, unit -> showListPanel(ListPanelDialog.TYPE_DOWNLOADS));
        chromeViewModel.isMyShotOnBoardingPending().observe(this, isPending -> {
            if (isPending != null && isPending) {
                showMyShotOnBoarding();
            }
        });
        chromeViewModel.getShowNightModeOnBoarding().observe(this, unit -> showNightModeOnBoarding());
        chromeViewModel.isNightMode().observe(this, isNightMode -> {
            if (isNightMode != null) {
                onNightModeEnabled(Settings.getInstance(this), isNightMode);
            }
        });
        chromeViewModel.getDriveDefaultBrowser().observe(this, unit -> driveDefaultBrowser());
        chromeViewModel.getExitApp().observe(this, unit -> onExitClicked());
        chromeViewModel.getOpenPreference().observe(this, unit -> openPreferences());
        chromeViewModel.getShowBookmarks().observe(this, unit -> showListPanel(ListPanelDialog.TYPE_BOOKMARKS));
        chromeViewModel.getShowHistory().observe(this, unit -> showListPanel(ListPanelDialog.TYPE_HISTORY));
        chromeViewModel.getShowScreenshots().observe(this, unit -> showListPanel(ListPanelDialog.TYPE_SCREENSHOTS));
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
    public void onQueryComplete(List<SessionManager.SessionWithState> states, String currentTabId) {
        chromeViewModel.onRestoreTabCountCompleted();
        getSessionManager().restore(states, currentTabId);
        Session currentTab = getSessionManager().getFocusSession();
        if (!Settings.getInstance(this).shouldShowFirstrun() && currentTab != null && !getSupportFragmentManager().isStateSaved()) {
            screenNavigator.restoreBrowserScreen(currentTab.getId());
        }
    }

    private void restoreTabsFromPersistence() {
        chromeViewModel.onRestoreTabCountStarted();
        TabModelStore.getInstance(this).getSavedTabs(this, this);
    }

    private void saveTabsToPersistence() {
        if (chromeViewModel.isTabRestoredComplete().getValue() != true) {
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
        PromotionDialog dialog = DialogUtils.createRateAppDialog(this);
        PromotionDialogExt.enqueue(dialogQueue, dialog, () -> {
            TelemetryWrapper.showRateApp(false);
            return null;
        });
    }

    @Override
    public void showRateAppNotification() {
        DialogUtils.showRateAppNotification(this);
        TelemetryWrapper.showRateApp(true);
    }

    @Override
    public void showShareAppDialog() {
        PromotionDialog dialog = DialogUtils.createShareAppDialog(this);
        PromotionDialogExt.enqueue(dialogQueue, dialog, () -> {
            TelemetryWrapper.showPromoteShareDialog();
            return null;
        });
    }

    @Override
    public void showPrivacyPolicyUpdateNotification() {
        DialogUtils.showPrivacyPolicyUpdateNotification(this);

    }

    @Override
    public void showRateAppDialogFromIntent() {
        PromotionDialog dialog = DialogUtils.createRateAppDialog(this);
        PromotionDialogExt.enqueue(dialogQueue, dialog, () -> {
            TelemetryWrapper.showRateApp(false);
            return null;
        });

        NotificationManagerCompat.from(this).cancel(NotificationId.LOVE_FIREFOX);

        // Reset extra after dialog displayed.
        if (getIntent().getExtras() != null) {
            getIntent().getExtras().putBoolean(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false);
        }
    }

    private boolean isInAppUpdateInstallIntent(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        return action != null && action.equals(ACTION_INSTALL_IN_APP_UPDATE);
    }

    private void installUpdateIfNeeded() {
        if (isInAppUpdateInstallIntent(getIntent())) {
            appUpdateManager.installUpdate(this);
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

    private void showNightModeOnBoarding() {
        View view = menu.findViewById(R.id.menu_night_mode);
        view.post(() -> DialogUtils.showSpotlight(
                MainActivity.this,
                view,
                dialog -> {
                },
                R.string.night_mode_on_boarding_message));
    }

    @VisibleForTesting
    @UiThread
    public void showMyShotOnBoarding() {
        Settings.getInstance(this).setNightModeSpotlight(false);
        View view = menu.findViewById(R.id.menu_screenshots);
        view.post(() -> {
            myshotOnBoardingDialog = DialogUtils.showMyShotOnBoarding(
                    MainActivity.this,
                    view,
                    dialog -> dismissAllMenus(),
                    v -> {
                        final String url = SupportUtils.getSumoURLForTopic(MainActivity.this, "screenshot-telemetry");
                        screenNavigator.showBrowserScreen(url, true, false);
                        dismissAllMenus();
                    });
            chromeViewModel.onMyShotOnBoardingDisplayed();
        });
        showMenu();
    }

    private ContentObserver downloadObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            downloadIndicatorViewModel.updateIndicator();
        }
    };
}
