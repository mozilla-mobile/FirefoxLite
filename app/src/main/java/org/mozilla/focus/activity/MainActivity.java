/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.fragment.ListPanelDialog;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.notification.NotificationId;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.screenshot.ScreenshotViewerActivity;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabModelStore;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewProvider;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionProvider;
import org.mozilla.focus.tabs.tabtray.TabTray;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.DialogUtils;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.focus.utils.FirebaseHelper;
import org.mozilla.focus.utils.FormatUtils;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ShortcutUtils;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.FragmentListener;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MainActivity extends LocaleAwareAppCompatActivity implements FragmentListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TabsSessionProvider.SessionHost, TabModelStore.AsyncQueryListener,
        ScreenNavigator.Provider {

    public static final String EXTRA_TEXT_SELECTION = "text_selection";

    private String pendingUrl;

    private BottomSheetDialog menu;
    private View nextButton;
    private View loadingButton;
    private View shareButton;
    private View refreshIcon;
    private View stopIcon;
    private View pinShortcut;

    private MainMediator mainMediator;
    private ScreenNavigator screenNavigator;

    private boolean safeForFragmentTransactions = false;
    private DialogFragment mDialogFragment;

    private BroadcastReceiver uiMessageReceiver;
    private static boolean sIsNewCreated = true;

    private TabsSession tabsSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseHelper.init(this);
        asyncInitialize();

        setContentView(R.layout.activity_main);
        initViews();
        initBroadcastReceivers();

        mainMediator = new MainMediator(this);
        screenNavigator = new ScreenNavigator(this);

        getSupportFragmentManager().addOnBackStackChangedListener(new BackStackListener());

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final String url = intent.getDataString();

                BrowsingSession.getInstance().loadCustomTabConfig(this, intent);

                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    pendingUrl = url;
                    this.mainMediator.showFirstRun();
                } else {
                    boolean openInNewTab = intent.getBooleanExtra(IntentUtils.EXTRA_OPEN_NEW_TAB,
                            false);
                    this.screenNavigator.showBrowserScreen(url, openInNewTab, true);
                }
            } else {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    this.mainMediator.showFirstRun();
                } else {
                    this.mainMediator.showHomeScreen();
                }
            }
        }
        restoreTabsFromPersistence();
        WebViewProvider.preload(this);

        if (sIsNewCreated) {
            sIsNewCreated = false;
            runPromotion(intent);
        }
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
    protected void onPostResume() {
        super.onPostResume();
        safeForFragmentTransactions = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver);

        safeForFragmentTransactions = false;
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

        if (runPromotionFromIntent(intent)) {
            // Don't run other promotion or other action if we already displayed above promotion
            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // We can't update our fragment right now because we need to wait until the activity is
            // resumed. So just remember this URL and load it in onResumeFragments().
            pendingUrl = intent.getDataString();
            // We don't want to see any menu is visible when processing open url request from Intent.ACTION_VIEW
            dismissAllMenus();
            TabTray.dismiss(getSupportFragmentManager());
        } else if (intent.getStringExtra(NotificationUtil.PUSH_OPEN_URL) != null) {
            pendingUrl = intent.getStringExtra(NotificationUtil.PUSH_OPEN_URL);
            dismissAllMenus();
            TabTray.dismiss(getSupportFragmentManager());
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

        setUpMenu();
    }

    private void runPromotion(final SafeIntent intent) {
        if (runPromotionFromIntent(intent)) {
            // Don't run other promotion if we already displayed above promotion
            return;
        }
        final Settings.EventHistory history = Settings.getInstance(this).getEventHistory();

        final boolean didShowRateDialog = history.contains(Settings.Event.ShowRateAppDialog);
        final boolean didShowShareDialog = history.contains(Settings.Event.ShowShareAppDialog);
        final boolean didDismissRateDialog = history.contains(Settings.Event.DismissRateAppDialog);
        final boolean didShowRateAppNotification = history.contains(Settings.Event.ShowRateAppNotification);
        final boolean isSurveyEnabled = AppConfigWrapper.isSurveyNotificationEnabled() &&
                !history.contains(Settings.Event.PostSurveyNotification);

        if (!didShowRateDialog || !didShowShareDialog || isSurveyEnabled || !didShowRateAppNotification) {
            history.add(Settings.Event.AppCreate);
        }
        final int appCreateCount = history.getCount(Settings.Event.AppCreate);

        if (!didShowRateDialog &&
                appCreateCount >= AppConfigWrapper.getRateDialogLaunchTimeThreshold()) {
            DialogUtils.showRateAppDialog(this);
            TelemetryWrapper.showFeedbackDialog();
        } else if (didDismissRateDialog && !didShowRateAppNotification && appCreateCount >=
                AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold()) {
            DialogUtils.showRateAppNotification(this);
            TelemetryWrapper.showRateAppNotification();
        } else if (!didShowShareDialog && appCreateCount >=
                AppConfigWrapper.getShareDialogLaunchTimeThreshold(didDismissRateDialog)) {
            DialogUtils.showShareAppDialog(this);
            TelemetryWrapper.showPromoteShareDialog();
        }

        if (isSurveyEnabled &&
                appCreateCount >= AppConfigWrapper.getSurveyNotificationLaunchTimeThreshold()) {
            postSurveyNotification();
            history.add(Settings.Event.PostSurveyNotification);
        }
    }

    // return true if promotion is already handled
    @CheckResult
    private boolean runPromotionFromIntent(final SafeIntent intent) {
        if (intent == null) {
            return false;
        }
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return false;
        }
        // When we receive this action, it means we need to show "Love Rocket" dialog
        final boolean loveRocket = bundle.getBoolean(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false);
        if (loveRocket) {
            DialogUtils.showRateAppDialog(this);
            NotificationManagerCompat.from(this).cancel(NotificationId.LOVE_FIREFOX);
            // Reset extra after dialog displayed.
            bundle.putBoolean(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false);
            return true;
        }
        return false;
    }

    private void postSurveyNotification() {
        Intent intent = IntentUtils.createInternalOpenUrlIntent(this,
                getSurveyUrl(), true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        final NotificationCompat.Builder builder = NotificationUtil.generateNotificationBuilder(this, pendingIntent)
                .setContentTitle(getString(R.string.survey_notification_title, "\uD83D\uDE4C"))
                .setContentText(getString(R.string.survey_notification_description))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        getString(R.string.survey_notification_description)));

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
        nextButton = menu.findViewById(R.id.action_next);
        loadingButton = menu.findViewById(R.id.action_loading);
        shareButton = menu.findViewById(R.id.action_share);
        refreshIcon = menu.findViewById(R.id.action_refresh);
        stopIcon = menu.findViewById(R.id.action_stop);
        pinShortcut = menu.findViewById(R.id.action_pin_shortcut);
        final boolean requestPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(this);
        if (!requestPinShortcutSupported) {
            pinShortcut.setVisibility(View.GONE);
        }
        menu.findViewById(R.id.menu_turbomode).setSelected(isTurboEnabled());
        menu.findViewById(R.id.menu_blockimg).setSelected(isBlockingImages());
    }

    @VisibleForTesting
    public BrowserFragment getVisibleBrowserFragment() {
        return screenNavigator.isBrowserInForeground() ? getBrowserFragment() : null;
    }

    private void showMenu() {
        updateMenu();
        menu.show();
    }

    private void updateMenu() {
        final BrowserFragment browserFragment = getVisibleBrowserFragment();
        final boolean canGoForward = browserFragment != null && browserFragment.canGoForward();

        setEnable(nextButton, canGoForward);
        setLoadingButton(browserFragment);
        setEnable(shareButton, browserFragment != null);
        setEnable(pinShortcut, browserFragment != null);
    }

    private boolean isTurboEnabled() {
        return Settings.getInstance(this).shouldUseTurboMode();
    }

    private boolean isBlockingImages() {
        return Settings.getInstance(this).shouldBlockImages();
    }

    private Fragment getTopHomeFragment() {
        final Fragment homeFragment = this.mainMediator.getTopHomeFragment();
        if (homeFragment == null) {
            return null;
        } else {
            return homeFragment;
        }
    }

    private void showListPanel(int type) {
        DialogFragment dialogFragment = ListPanelDialog.newInstance(type);
        dialogFragment.setCancelable(true);
        final Fragment homeFragment = getTopHomeFragment();
        if (homeFragment != null) {
            dialogFragment.setTargetFragment(homeFragment, HomeFragment.REFRESH_REQUEST_CODE);
        }
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
            case R.id.action_next:
            case R.id.action_loading:
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
        if (settings.getMenuPreferenceClickCount() == AppConfigWrapper.getDriveDefaultBrowserFromMenuSettingThreshold()) {
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
        tabsSession.destroy();
        super.onDestroy();
    }

    private void onPreferenceClicked() {
        openPreferences();
    }

    private void onExitClicked() {
        finish();
    }

    private void onDownloadClicked() {
        showListPanel(ListPanelDialog.TYPE_DOWNLOADS);
    }

    private void onHistoryClicked() {
        showListPanel(ListPanelDialog.TYPE_HISTORY);
    }

    private void onScreenshotsClicked() {
        showListPanel(ListPanelDialog.TYPE_SCREENSHOTS);
    }

    private void onDeleteClicked() {
        final long diff = FileUtils.clearCache(this);
        final int stringId = (diff < 0) ? R.string.message_clear_cache_fail : R.string.message_cleared_cached;
        final String msg = getString(stringId, FormatUtils.getReadableStringFromFileSize(diff));
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private BrowserFragment getBrowserFragment() {
        return (BrowserFragment) getSupportFragmentManager().findFragmentById(R.id.browser);
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
                        mDialogFragment.dismiss();
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
        final Tab focusTab = getTabsSession().getFocusTab();
        if (focusTab == null) {
            return;
        }
        final String url = focusTab.getUrl();
        // If we pin an invalid url as shortcut, the app will not function properly.
        // TODO: only enable the bottom menu item if the page is valid and loaded.
        if (!UrlUtils.isUrl(url)) {
            return;
        }
        final Bitmap bitmap = focusTab.getFavicon();
        final Intent shortcut = new Intent(Intent.ACTION_VIEW);
        shortcut.setClass(this, MainActivity.class);
        shortcut.setData(Uri.parse(url));

        ShortcutUtils.requestPinShortcut(this, shortcut, focusTab.getTitle(), url, bitmap);
    }

    @Override
    public void onBackPressed() {
        if (!safeForFragmentTransactions) {
            return;
        }

        BrowserFragment browserFragment = getVisibleBrowserFragment();
        if (browserFragment != null && browserFragment.onBackPressed()) {
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
            this.mainMediator.showHomeScreen();
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
            case SHOW_URL_INPUT:
                if (!safeForFragmentTransactions) {
                    return;
                }
                final String url = (payload != null) ? payload.toString() : null;
                this.mainMediator.showUrlInput(url);
                break;
            case DISMISS_URL_INPUT:
                this.mainMediator.dismissUrlInput();
                break;
            case FRAGMENT_STARTED:
                if ((payload != null) && (payload instanceof String)) {
                    this.mainMediator.onFragmentStarted(((String) payload).toLowerCase(Locale.ROOT));
                }
                break;
            case FRAGMENT_STOPPED:
                if ((payload != null) && (payload instanceof String)) {
                    this.mainMediator.onFragmentStopped(((String) payload).toLowerCase(Locale.ROOT));
                }
                break;
            case SHOW_TAB_TRAY:
                TabTray.show(getSupportFragmentManager());
                break;
            default:
                break;
        }
    }

    @Override
    public ScreenNavigator getScreenNavigator() {
        return screenNavigator;
    }

    public FirstrunFragment createFirstRunFragment() {
        return FirstrunFragment.create();
    }

    public UrlInputFragment createUrlInputFragment(@Nullable String url, String parentFragmentTag) {
        final UrlInputFragment fragment = UrlInputFragment.create(url, parentFragmentTag);
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
        DownloadInfoManager.getInstance().queryByRowId(rowId, new DownloadInfoManager.AsyncQueryListener() {
            @Override
            public void onQueryComplete(List downloadInfoList) {
                if (downloadInfoList.size() > 0) {
                    final DownloadInfo downloadInfo = (DownloadInfo) downloadInfoList.get(0);
                    if (!downloadInfo.existInDownloadManager()) {
                        // Should never happen
                        final String msg = "File entry disappeared after being downloaded";
                        throw new IllegalStateException(msg);
                    }
                    final View container = findViewById(R.id.container);
                    String completedStr = getString(R.string.download_completed, downloadInfo.getFileName());
                    Snackbar.make(container, completedStr, Snackbar.LENGTH_LONG)
                            .setAction(R.string.open, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    IntentUtils.intentOpenFile(MainActivity.this, downloadInfo.getFileUri(), downloadInfo.getMimeType());
                                }
                            })
                            .show();
                }
            }
        });
    }

    @Override
    public TabsSession getTabsSession() {
        // TODO: Find a proper place to allocate and init TabsSession
        if (tabsSession == null) {
            final TabViewProvider provider = new MainTabViewProvider(this);
            tabsSession = new TabsSession(provider);
        }
        return tabsSession;
    }

    @Override
    public void onQueryComplete(List<TabModel> tabModelList, String currentTabId) {
        getTabsSession().restoreTabs(tabModelList, currentTabId);
        Tab currentTab = getTabsSession().getFocusTab();
        if (currentTab != null && safeForFragmentTransactions) {
            screenNavigator.restoreBrowserScreen(currentTab.getId());
        }
    }

    private void restoreTabsFromPersistence() {
        TabModelStore.getInstance(this).getSavedTabs(this, this);
    }

    private void saveTabsToPersistence() {
        List<TabModel> tabModelListForPersistence = getTabsSession().getTabModelListForPersistence();
        final String currentTabId = (getTabsSession().getFocusTab() != null)
                ? getTabsSession().getFocusTab().getId()
                : null;

        if (tabModelListForPersistence != null) {
            TabModelStore.getInstance(this).saveTabs(this, tabModelListForPersistence, currentTabId, null);
        }
    }

    // a TabViewProvider and it should only be used in this activity
    private static class MainTabViewProvider implements TabViewProvider {
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

    private class BackStackListener implements FragmentManager.OnBackStackChangedListener {
        @Override
        public void onBackStackChanged() {
            final BrowserFragment fragment = getBrowserFragment();
            final Fragment top = MainActivity.this.mainMediator.getTopFragment();
            if (top == null) {
                fragment.goForeground();
            } else {
                fragment.goBackground();
            }
        }
    }
}
