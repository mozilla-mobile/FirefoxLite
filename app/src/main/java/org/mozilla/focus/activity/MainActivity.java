/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.fragment.ListPanelDialog;
import org.mozilla.focus.fragment.ScreenCaptureDialogFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.notification.NotificationId;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.permission.PermissionHandle;
import org.mozilla.focus.permission.PermissionHandler;
import org.mozilla.focus.screenshot.ScreenshotCaptureTask;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.screenshot.ScreenshotViewerActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.DialogUtils;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.focus.utils.FormatUtils;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.FragmentListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class MainActivity extends LocaleAwareAppCompatActivity implements FragmentListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String EXTRA_TEXT_SELECTION = "text_selection";
    private static final Handler HANDLER = new Handler();

    private String pendingUrl;

    private BottomSheetDialog menu;
    private View nextButton;
    private View loadingButton;
    private View shareButton;
    private View captureButton;
    private View refreshIcon;
    private View stopIcon;

    private MainMediator mediator;
    private boolean safeForFragmentTransactions = false;
    private boolean hasPendingScreenCaptureTask = false;
    private DialogFragment mDialogFragment;

    private BroadcastReceiver uiMessageReceiver;
    private static boolean sIsNewCreated = true;

    private PermissionHandler permissionHandler;
    private static final int ACTION_CAPTURE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asyncInitialize();

        setContentView(R.layout.activity_main);
        initViews();
        initBroadcastReceivers();

        mediator = new MainMediator(this);
        permissionHandler = new PermissionHandler(new PermissionHandle() {
            @Override
            public void doActionDirect(String permission, int actionId, Parcelable params) {
                if (actionId == ACTION_CAPTURE) {
                    BrowserFragment browserFragment = getBrowserFragment();
                    if (browserFragment == null || !browserFragment.isVisible()) {
                        return;
                    }
                    showLoadingAndCapture(browserFragment);
                }
            }

            private void doCaptureGranted() {
                BrowserFragment browserFragment = getBrowserFragment();
                if (browserFragment == null || !browserFragment.isVisible()) {
                    return;
                }
                hasPendingScreenCaptureTask = true;
            }

            @Override
            public void doActionGranted(String permission, int actionId, Parcelable params) {
                if (actionId == ACTION_CAPTURE) {
                    doCaptureGranted();
                }
            }

            @Override
            public void doActionSetting(String permission, int actionId, Parcelable params) {
                if (actionId == ACTION_CAPTURE) {
                    doCaptureGranted();
                }
            }

            @Override
            public void doActionNoPermission(String permission, int actionId, Parcelable params) {
                // Do nothing
            }

            @Override
            public int getDoNotAskAgainDialogString(int actionId) {
                if (actionId == ACTION_CAPTURE ) {
                    return R.string.permission_dialog_msg_storage;
                } else {
                    throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public Snackbar makeAskAgainSnackBar(int actionId) {
                return PermissionHandler.makeAskAgainSnackBar(MainActivity.this, findViewById(R.id.container), getAskAgainSnackBarString(actionId));
            }

            private int getAskAgainSnackBarString(int actionId) {
                if (actionId == ACTION_CAPTURE ) {
                    return R.string.permission_toast_storage;
                } else {
                    throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public void requestPermissions(int actionId) {
                if (actionId == ACTION_CAPTURE) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, actionId);
                }
            }
        });

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final String url = intent.getDataString();

                BrowsingSession.getInstance().loadCustomTabConfig(this, intent);

                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    pendingUrl = url;
                    this.mediator.showFirstRun();
                } else {
                    boolean isFromInternal = intent.getBooleanExtra(IntentUtils.EXTRA_IS_INTERNAL_REQUEST, false);
                    this.mediator.showBrowserScreen(url, isFromInternal ? false : true);
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

        if (sIsNewCreated) {
            sIsNewCreated = false;
            onNewCreate();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        permissionHandler.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        permissionHandler.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
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
        if (hasPendingScreenCaptureTask) {
            final BrowserFragment browserFragment = getBrowserFragment();
            if (browserFragment != null && browserFragment.isVisible()) {
                showLoadingAndCapture(browserFragment);
            }
            hasPendingScreenCaptureTask = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver);

        safeForFragmentTransactions = false;
        TelemetryWrapper.stopSession();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
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
            // We don't want to see any menu is visible when processing open url request from Intent.ACTION_VIEW
            dismissAllMenus();
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
            boolean isFromInternal = intent != null && intent.getBooleanExtra(IntentUtils.EXTRA_IS_INTERNAL_REQUEST, false);
            this.mediator.showBrowserScreen(pendingUrl, isFromInternal ? false : true);
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

    private void onNewCreate() {
        Settings.EventHistory history = Settings.getInstance(this).getEventHistory();

        boolean didShowRateDialog = history.contains(Settings.Event.ShowRateAppDialog);
        boolean didShowShareDialog = history.contains(Settings.Event.ShowShareAppDialog);
        boolean didPostSurvey = history.contains(Settings.Event.PostSurveyNotification);

        if (!didShowRateDialog || !didShowShareDialog || !didPostSurvey) {
            history.add(Settings.Event.AppCreate);
        }
        int appCreateCount = history.getCount(Settings.Event.AppCreate);

        if (!didShowRateDialog && appCreateCount >= DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_APP) {
            DialogUtils.showRateAppDialog(this);
            TelemetryWrapper.showFeedbackDialog();
        } else if (!didShowShareDialog && appCreateCount >= DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_APP) {
            DialogUtils.showShareAppDialog(this);
            TelemetryWrapper.showPromoteShareDialog();
        }

        if (appCreateCount >= 3 && !didPostSurvey) {
            postSurveyNotification();
            history.add(Settings.Event.PostSurveyNotification);
        }
    }

    private void postSurveyNotification() {
        Intent intent = IntentUtils.createInternalOpenUrlIntent(this,
                getSurveyUrl());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.survey_notification_title, "\uD83D\uDE4C"))
                .setContentText(getString(R.string.survey_notification_description))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        getString(R.string.survey_notification_description)))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0]);

        if (BuildCompat.isAtLeastN()) {
            builder.setColor(Color.parseColor("#0060df"))
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setShowWhen(false);
        } else {
            builder.setColor(Color.parseColor("#00c8d7"));
        }

        NotificationUtil.sendNotification(this, NotificationId.SURVEY_ON_3RD_LAUNCH, builder);
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
        nextButton = menu.findViewById(R.id.action_next);
        loadingButton = menu.findViewById(R.id.action_loading);
        shareButton = menu.findViewById(R.id.action_share);
        captureButton = menu.findViewById(R.id.capture_page);
        refreshIcon = menu.findViewById(R.id.action_refresh);
        stopIcon = menu.findViewById(R.id.action_stop);
        menu.findViewById(R.id.menu_turbomode).setSelected(isTurboEnabled());
        menu.findViewById(R.id.menu_blockimg).setSelected(isBlockingImages());
    }

    private BrowserFragment getVisibleBrowserFragment() {
        final BrowserFragment browserFragment = getBrowserFragment();
        if (browserFragment == null || !browserFragment.isVisible()) {
            return null;
        } else {
            return browserFragment;
        }
    }

    private void showMenu() {
        updateMenu();
        menu.show();
    }

    private void updateMenu() {
        final BrowserFragment browserFragment = getVisibleBrowserFragment();
        final boolean hasLoadedPage = browserFragment != null && !browserFragment.isLoading();
        final boolean canGoForward = browserFragment != null && browserFragment.canGoForward();

        setEnable(nextButton, canGoForward);
        setLoadingButton(browserFragment);
        setEnable(shareButton, browserFragment != null);
        setEnable(captureButton, hasLoadedPage);
    }

    private boolean isTurboEnabled() {
        return Settings.getInstance(this).shouldUseTurboMode();
    }

    private boolean isBlockingImages() {
        return Settings.getInstance(this).shouldBlockImages();
    }

    private Fragment getTopHomeFragment() {
        final Fragment homeFragment = this.mediator.getTopHomeFragmet();
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
            case R.id.capture_page:
                onMenuBrowsingItemClicked(v);
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuItemClicked() is only for" +
                        " known ids");
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
            case R.id.capture_page:
                Settings.getInstance(this).setScreenshotOnBoardingDone();
                onCapturePageClicked();
                TelemetryWrapper.clickToolbarCapture();
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuBrowsingItemClicked() is" +
                        " only for known ids");
        }
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

    private void onStopClicked(final BrowserFragment browserFragment) {
        browserFragment.stop();
    }

    private void onCapturePageClicked() {
        permissionHandler.tryAction(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_CAPTURE, null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Only refresh when disabling turbo mode
        if (this.getResources().getString(R.string.pref_key_turbo_mode).equals(key)) {
            final boolean turboEnabled = isTurboEnabled();
            BrowserFragment browserFragment = getVisibleBrowserFragment();
            if (browserFragment != null) {
                browserFragment.setBlockingEnabled(turboEnabled);
                // Reload if we're closing Turbo mode since we should be fixing something
                if (!turboEnabled) {
                    browserFragment.reload();
                }
            }
            menu.findViewById(R.id.menu_turbomode).setSelected(turboEnabled);
        } else if (this.getResources().getString(R.string.pref_key_performance_block_images).equals(key)) {
            final boolean blockingImages = isBlockingImages();
            if (getVisibleBrowserFragment() != null) {
                getVisibleBrowserFragment().reload();
            }
            menu.findViewById(R.id.menu_blockimg).setSelected(blockingImages);
        }
        // For turbo mode, a automatic refresh is done when we disable block image.
    }

    private static final class CaptureRunnable extends ScreenshotCaptureTask implements Runnable, BrowserFragment.ScreenshotCallback {

        final WeakReference<Context> refContext;
        final WeakReference<BrowserFragment> refBrowserFragment;
        final WeakReference<ScreenCaptureDialogFragment> refScreenCaptureDialogFragment;
        final WeakReference<View> refContainerView;

        CaptureRunnable(Context context, BrowserFragment browserFragment, ScreenCaptureDialogFragment screenCaptureDialogFragment, View container) {
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
        }

    }

    private void showLoadingAndCapture(final BrowserFragment browserFragment) {
        if (!safeForFragmentTransactions) {
            return;
        }
        hasPendingScreenCaptureTask = false;
        final ScreenCaptureDialogFragment capturingFragment = ScreenCaptureDialogFragment.newInstance();
        capturingFragment.show(getSupportFragmentManager(), "capturingFragment");

        final int WAIT_INTERVAL = 150;
        // Post delay to wait for Dialog to show
        HANDLER.postDelayed(new CaptureRunnable(MainActivity.this, browserFragment, capturingFragment, findViewById(R.id.container)), WAIT_INTERVAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHandler.onActivityResult(this, requestCode, resultCode, data);

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
                    onNotified(null, TYPE.OPEN_URL, url);
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

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (name.equals(IWebView.class.getName())) {
            View v = WebViewProvider.create(this, attrs);
            return v;
        }

        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public void onBackPressed() {
        if (!safeForFragmentTransactions) {
            return;
        }
        if (this.mediator.handleBackKey()) {
            return;
        }
        super.onBackPressed();
    }

    public void firstrunFinished() {
        if (pendingUrl != null) {
            // We have received an URL in onNewIntent(). Let's load it now.
            this.mediator.showBrowserScreen(pendingUrl, true);
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
                    this.mediator.showBrowserScreen(payload.toString(), false);
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
            case UPDATE_MENU:
                this.updateMenu();
                break;
            case SHOW_URL_INPUT:
                if (!safeForFragmentTransactions) {
                    return;
                }
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
            case SHOW_SCREENSHOT_HINT:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DialogUtils.showScreenshotOnBoardingDialog(MainActivity.this);
                        TelemetryWrapper.showPromoteScreenShotDialog();
                    }
                });
                break;
        }
    }

    public FirstrunFragment createFirstRunFragment() {
        return FirstrunFragment.create();
    }

    public BrowserFragment createBrowserFragment(@NonNull String url) {
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
}
