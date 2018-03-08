/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.menu.WebContextMenu;
import org.mozilla.focus.permission.PermissionHandle;
import org.mozilla.focus.permission.PermissionHandler;
import org.mozilla.focus.screenshot.CaptureRunnable;
import org.mozilla.focus.screenshot.ScreenshotObserver;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabCounter;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabsChromeListener;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionProvider;
import org.mozilla.focus.tabs.TabsViewListener;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.ColorUtils;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.DrawableUtils;
import org.mozilla.focus.utils.FileChooseAction;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.CustomTabConfig;
import org.mozilla.focus.web.Download;
import org.mozilla.focus.widget.AnimatedProgressBar;
import org.mozilla.focus.widget.BackKeyHandleable;
import org.mozilla.focus.widget.FragmentListener;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Fragment for displaying the browser UI.
 */
public class BrowserFragment extends LocaleAwareFragment implements View.OnClickListener,
        BackKeyHandleable, ScreenshotObserver.OnScreenshotListener {

    public static final String FRAGMENT_TAG = "browser";
    private static final Handler HANDLER = new Handler();

    private static final int ANIMATION_DURATION = 300;

    private static final int SITE_GLOBE = 0;
    private static final int SITE_LOCK = 1;

    private final static int NONE = -1;
    private int systemVisibility = NONE;

    private DownloadCallback downloadCallback = new DownloadCallback();

    private static final int BUNDLE_MAX_SIZE = 300 * 1000; // 300K

    private ViewGroup webViewSlot;
    private TabsSession tabsSession;

    private View backgroundView;
    private TransitionDrawable backgroundTransition;
    private TabCounter tabCounter;
    private TextView urlView;
    private AnimatedProgressBar progressView;
    private ImageView siteIdentity;
    private Dialog webContextMenu;

    //GeoLocationPermission
    private String geolocationOrigin;
    private GeolocationPermissions.Callback geolocationCallback;
    private AlertDialog geoDialog;

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private ViewGroup videoContainer;

    /**
     * Container containing the browser chrome and web content.
     */
    private View browserContainer;

    private TabView.FullscreenCallback fullscreenCallback;

    private boolean isLoading = false;

    // Set an initial WeakReference so we never have to handle loadStateListenerWeakReference being null
    // (i.e. so we can always just .get()).
    private WeakReference<LoadStateListener> loadStateListenerWeakReference = new WeakReference<>(null);

    // pending action for file-choosing
    private FileChooseAction fileChooseAction;

    private ScreenshotObserver screenshotObserver;

    private PermissionHandler permissionHandler;
    private static final int ACTION_DOWNLOAD = 0;
    private static final int ACTION_PICK_FILE = 1;
    private static final int ACTION_GEO_LOCATION = 2;
    private static final int ACTION_CAPTURE = 3;

    private boolean hasPendingScreenCaptureTask = false;

    final TabsContentListener tabsContentListener = new TabsContentListener();

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            permissionHandler.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        permissionHandler = new PermissionHandler(new PermissionHandle() {
            @Override
            public void doActionDirect(String permission, int actionId, Parcelable params) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                        if (getContext() == null) {
                            Log.w(FRAGMENT_TAG, "No context to use, abort callback onDownloadStart");
                            return;
                        }

                        Download download = (Download) params;

                        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            // We do have the permission to write to the external storage. Proceed with the download.
                            queueDownload(download);
                        }
                        break;
                    case ACTION_PICK_FILE:
                        fileChooseAction.startChooserActivity();
                        break;
                    case ACTION_GEO_LOCATION:
                        showGeolocationPermissionPrompt();
                        break;
                    case ACTION_CAPTURE:
                        showLoadingAndCapture();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown actionId");
                }
            }


            private void actionDownloadGranted(Parcelable parcelable) {
                Download download = (Download) parcelable;
                queueDownload(download);
            }

            private void actionPickFileGranted() {
                if (fileChooseAction != null) {
                    fileChooseAction.startChooserActivity();
                }
            }

            private void actionCaptureGranted() {
                hasPendingScreenCaptureTask = true;
            }

            private void doActionGrantedOrSetting(String permission, int actionId, Parcelable params) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                        actionDownloadGranted(params);
                        break;
                    case ACTION_PICK_FILE:
                        actionPickFileGranted();
                        break;
                    case ACTION_GEO_LOCATION:
                        showGeolocationPermissionPrompt();
                        break;
                    case ACTION_CAPTURE:
                        actionCaptureGranted();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown actionId");
                }
            }

            @Override
            public void doActionGranted(String permission, int actionId, Parcelable params) {
                doActionGrantedOrSetting(permission, actionId, params);
            }

            @Override
            public void doActionSetting(String permission, int actionId, Parcelable params) {
                doActionGrantedOrSetting(permission, actionId, params);
            }

            @Override
            public void doActionNoPermission(String permission, int actionId, Parcelable params) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                        // Do nothing
                        break;
                    case ACTION_PICK_FILE:
                        if (fileChooseAction != null) {
                            fileChooseAction.cancel();
                            fileChooseAction = null;
                        }
                        break;
                    case ACTION_GEO_LOCATION:
                        if (geolocationCallback != null) {
                            rejectGeoRequest();
                        }
                        break;
                    case ACTION_CAPTURE:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown actionId");
                }
            }

            @Override
            public int getDoNotAskAgainDialogString(int actionId) {
                if (actionId == ACTION_DOWNLOAD || actionId == ACTION_PICK_FILE || actionId == ACTION_CAPTURE) {
                    return R.string.permission_dialog_msg_storage;
                } else if (actionId == ACTION_GEO_LOCATION) {
                    return R.string.permission_dialog_msg_location;
                } else {
                    throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public Snackbar makeAskAgainSnackBar(int actionId) {
                return PermissionHandler.makeAskAgainSnackBar(BrowserFragment.this, getActivity().findViewById(R.id.container), getAskAgainSnackBarString(actionId));
            }

            private int getAskAgainSnackBarString(int actionId) {
                if (actionId == ACTION_DOWNLOAD || actionId == ACTION_PICK_FILE || actionId == ACTION_CAPTURE) {
                    return R.string.permission_toast_storage;
                } else if (actionId == ACTION_GEO_LOCATION) {
                    return R.string.permission_toast_location;
                } else {
                    throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public void requestPermissions(int actionId) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                    case ACTION_CAPTURE:
                        BrowserFragment.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, actionId);
                        break;
                    case ACTION_PICK_FILE:
                        BrowserFragment.this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, actionId);
                        break;
                    case ACTION_GEO_LOCATION:
                        BrowserFragment.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, actionId);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown Action");
                }
            }
        });
    }

    @Override
    public void onPause() {
        tabsSession.pause();
        super.onPause();
        if (screenshotObserver != null) {
            screenshotObserver.stop();
        }
    }

    @Override
    public void applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        final WebView unneeded = new WebView(getContext());
        unneeded.destroy();
    }

    @Override
    public void onResume() {
        tabsSession.resume();
        super.onResume();
        if (hasPendingScreenCaptureTask) {
            showLoadingAndCapture();
            hasPendingScreenCaptureTask = false;
        }
        if (Settings.getInstance(getActivity()).shouldShowScreenshotOnBoarding()) {
            screenshotObserver = new ScreenshotObserver(getActivity(), this);
            screenshotObserver.start();
        }
    }

    private void updateURL(final String url) {
        if (UrlUtils.isInternalErrorURL(url)) {
            return;
        }

        urlView.setText(UrlUtils.stripUserInfo(url));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_browser, container, false);

        videoContainer = (ViewGroup) view.findViewById(R.id.video_container);
        browserContainer = view.findViewById(R.id.browser_container);

        urlView = (TextView) view.findViewById(R.id.display_url);

        backgroundView = view.findViewById(R.id.background);
        backgroundTransition = (TransitionDrawable) backgroundView.getBackground();

        tabCounter = view.findViewById(R.id.btn_tab_tray);
        final View newTabBtn = view.findViewById(R.id.btn_open_new_tab);
        final View searchBtn = view.findViewById(R.id.btn_search);
        final View captureBtn = view.findViewById(R.id.btn_capture);
        final View menuBtn = view.findViewById(R.id.btn_menu);
        if (tabCounter != null) {
            tabCounter.setOnClickListener(this);
        }
        if (newTabBtn != null) {
            newTabBtn.setOnClickListener(this);
        }
        if (searchBtn != null) {
            searchBtn.setOnClickListener(this);
        }
        if (captureBtn != null) {
            captureBtn.setOnClickListener(this);
        }
        if (menuBtn != null) {
            menuBtn.setOnClickListener(this);
        }

        siteIdentity = (ImageView) view.findViewById(R.id.site_identity);

        progressView = (AnimatedProgressBar) view.findViewById(R.id.progress);

        if (BrowsingSession.getInstance().isCustomTab()) {
            initialiseCustomTabUi(view);
        } else {
            initialiseNormalBrowserUi();
        }

        webViewSlot = (ViewGroup) view.findViewById(R.id.webview_slot);

        tabsSession = TabsSessionProvider.getOrThrow(getActivity());

        tabsSession.addTabsViewListener(this.tabsContentListener);
        tabsSession.addTabsChromeListener(this.tabsContentListener);
        tabsSession.setDownloadCallback(downloadCallback);

        if (tabCounter != null) {
            tabCounter.setCount(tabsSession.getTabsCount());
        }

        return view;
    }

    @Override
    public void onViewCreated(@Nullable View container, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(container, savedInstanceState);

        // restore WebView state
        if (savedInstanceState != null) {
            // Fragment was destroyed
            // FIXME: Obviously, only restore current tab is not enough
            if (tabsSession.getCurrentTab() != null) {
                tabsSession.getCurrentTab().getTabView().restoreViewState(savedInstanceState);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        permissionHandler.onActivityResult(getActivity(), requestCode, resultCode, data);
        if (requestCode == FileChooseAction.REQUEST_CODE_CHOOSE_FILE) {
            final boolean done = (fileChooseAction == null) || fileChooseAction.onFileChose(resultCode, data);
            if (done) {
                fileChooseAction = null;
            }
        }
    }

    public void onCaptureClicked() {
        permissionHandler.tryAction(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_CAPTURE, null);
    }

    private void initialiseNormalBrowserUi() {
        urlView.setOnClickListener(this);
    }

    private void initialiseCustomTabUi(final @NonNull View view) {
        final CustomTabConfig customTabConfig = BrowsingSession.getInstance().getCustomTabConfig();

        final int textColor;

        final View toolbar = view.findViewById(R.id.urlbar);
        if (customTabConfig.toolbarColor != null) {
            toolbar.setBackgroundColor(customTabConfig.toolbarColor);

            textColor = ColorUtils.getReadableTextColor(customTabConfig.toolbarColor);
            urlView.setTextColor(textColor);
        } else {
            textColor = Color.WHITE;
        }

        final ImageView closeButton = (ImageView) view.findViewById(R.id.customtab_close);

        closeButton.setVisibility(View.VISIBLE);
        closeButton.setOnClickListener(this);

        if (customTabConfig.closeButtonIcon != null) {
            closeButton.setImageBitmap(customTabConfig.closeButtonIcon);
        } else {
            // Always set the icon in case it's been overridden by a previous CT invocation
            final Drawable closeIcon = DrawableUtils.loadAndTintDrawable(getContext(), R.drawable.ic_close, textColor);

            closeButton.setImageDrawable(closeIcon);
        }

        if (customTabConfig.disableUrlbarHiding) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            params.setScrollFlags(0);
        }

        if (customTabConfig.actionButtonConfig != null) {
            final ImageButton actionButton = (ImageButton) view.findViewById(R.id.customtab_actionbutton);
            actionButton.setVisibility(View.VISIBLE);

            actionButton.setImageBitmap(customTabConfig.actionButtonConfig.icon);
            actionButton.setContentDescription(customTabConfig.actionButtonConfig.description);

            final PendingIntent pendingIntent = customTabConfig.actionButtonConfig.pendingIntent;

            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        final Intent intent = new Intent();
                        intent.setData(Uri.parse(getUrl()));

                        pendingIntent.send(getContext(), 0, intent);
                    } catch (PendingIntent.CanceledException e) {
                        // There's really nothing we can do here...
                    }
                }
            });
        }

        // We need to tint some icons.. We already tinted the close button above. Let's tint our other icons too.
        final Drawable tintedIcon = DrawableUtils.loadAndTintDrawable(getContext(), R.drawable.ic_lock, textColor);
        siteIdentity.setImageDrawable(tintedIcon);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        permissionHandler.onSaveInstanceState(outState);
        if (tabsSession.getCurrentTab() != null) {
            tabsSession.getCurrentTab().getTabView().saveViewState(outState);
        }

        // Workaround for #1107 TransactionTooLargeException
        // since Android N, system throws a exception rather than just a warning(then drop bundle)
        // To set a threshold for dropping WebView state manually
        // refer: https://issuetracker.google.com/issues/37103380
        final String key = "WEBVIEW_CHROMIUM_STATE";
        if (outState.containsKey(key)) {
            final int size = outState.getByteArray(key).length;
            if (size > BUNDLE_MAX_SIZE) {
                outState.remove(key);
            }
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        notifyParent(FragmentListener.TYPE.FRAGMENT_STARTED, FRAGMENT_TAG);
    }

    @Override
    public void onStop() {
        if (systemVisibility != NONE) {
            final Tab tab = tabsSession.getCurrentTab();
            if (tab != null && tab.getTabView() != null) {
                tab.getTabView().performExitFullScreen();
            }
        }
        dismissGeoDialog();
        super.onStop();
        notifyParent(FragmentListener.TYPE.FRAGMENT_STOPPED, FRAGMENT_TAG);
    }

    @Override
    public void onDestroy() {
        tabsSession.removeTabsViewListener(this.tabsContentListener);
        tabsSession.removeTabsChromeListener(this.tabsContentListener);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public interface LoadStateListener {
        void isLoadingChanged(boolean isLoading);
    }

    /**
     * Set a (singular) LoadStateListener. Only one listener is supported at any given time. Setting
     * a new listener means any previously set listeners will be dropped. This is only intended
     * to be used by NavigationItemViewHolder. If you want to use this method for any other
     * parts of the codebase, please extend it to handle a list of listeners. (We would also need
     * to automatically clean up expired listeners from that list, probably when adding to that list.)
     *
     * @param listener The listener to notify of load state changes. Only a weak reference will be kept,
     *                 no more calls will be sent once the listener is garbage collected.
     */
    public void setIsLoadingListener(final LoadStateListener listener) {
        loadStateListenerWeakReference = new WeakReference<>(listener);
    }

    private void showLoadingAndCapture() {
        if (!isResumed()) {
            return;
        }
        hasPendingScreenCaptureTask = false;
        final ScreenCaptureDialogFragment capturingFragment = ScreenCaptureDialogFragment.newInstance();
        capturingFragment.show(getChildFragmentManager(), "capturingFragment");

        final int WAIT_INTERVAL = 150;
        // Post delay to wait for Dialog to show
        HANDLER.postDelayed(new CaptureRunnable(getContext(), this, capturingFragment, getActivity().findViewById(R.id.container)), WAIT_INTERVAL);
    }

    private void updateIsLoading(final boolean isLoading) {
        this.isLoading = isLoading;
        final BrowserFragment.LoadStateListener currentListener = loadStateListenerWeakReference.get();
        if (currentListener != null) {
            currentListener.isLoadingChanged(isLoading);
        }
    }

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    private int switchToImmersiveMode() {
        final Activity activity = getActivity();
        Window window = activity.getWindow();
        final int original = window.getDecorView().getSystemUiVisibility();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        return original;
    }

    /**
     * Show the system bars again.
     */
    private void exitImmersiveMode(int visibility) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(visibility);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionHandler.onRequestPermissionsResult(getContext(), requestCode, permissions, grantResults);
    }

    /**
     * Use Android's Download Manager to queue this download.
     */
    private void queueDownload(Download download) {
        if (download == null) {
            return;
        }

        final Context context = getContext();
        if (context == null) {
            return;
        }

        final String cookie = CookieManager.getInstance().getCookie(download.getUrl());
        final String fileName = URLUtil.guessFileName(
                download.getUrl(), download.getContentDisposition(), download.getMimeType());

        // so far each download always return null even for an image.
        // But we might move downloaded file to another directory.
        // So, for now we always save file to DIRECTORY_DOWNLOADS
        final String dir = Environment.DIRECTORY_DOWNLOADS;

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(getContext(),
                    R.string.message_storage_unavailable_cancel_download,
                    Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // block non-http/https download links
        if (!URLUtil.isNetworkUrl(download.getUrl())) {
            Toast.makeText(getContext(), R.string.download_file_not_supported, Toast.LENGTH_LONG).show();
            return;
        }

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download.getUrl()))
                .addRequestHeader("User-Agent", download.getUserAgent())
                .addRequestHeader("Cookie", cookie)
                .addRequestHeader("Referer", getUrl())
                .setDestinationInExternalPublicDir(dir, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType(download.getMimeType());

        request.allowScanningByMediaScanner();

        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final Long downloadId = manager.enqueue(request);

        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setDownloadId(downloadId);
        // On Pixel, When downloading downloaded content which is still available, DownloadManager
        // returns the previous download id.
        // For that case we remove the old entry and re-insert a new one to move it to the top.
        // (Note that this is not the case for devices like Samsung, I have not verified yet if this
        // is a because of on those devices we move files to SDcard or if this is true even if the
        // file is not moved.)
        if (!DownloadInfoManager.getInstance().recordExists(downloadId)) {
            DownloadInfoManager.getInstance().insert(downloadInfo, new DownloadInfoManager.AsyncInsertListener() {
                @Override
                public void onInsertComplete(long id) {
                    DownloadInfoManager.notifyRowUpdated(getContext(), id);
                }
            });
        } else {
            DownloadInfoManager.getInstance().queryByDownloadId(downloadId, new DownloadInfoManager.AsyncQueryListener() {
                @Override
                public void onQueryComplete(List downloadInfoList) {
                    if (!downloadInfoList.isEmpty()) {
                        DownloadInfo info = (DownloadInfo) downloadInfoList.get(0);
                        DownloadInfoManager.getInstance().delete(info.getRowId(), null);
                        DownloadInfoManager.getInstance().insert(info, new DownloadInfoManager.AsyncInsertListener() {
                            @Override
                            public void onInsertComplete(long id) {
                                DownloadInfoManager.notifyRowUpdated(getContext(), id);
                                final Intent broadcastIntent = new Intent(Constants.ACTION_NOTIFY_RELOCATE_FINISH);
                                broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION);
                                broadcastIntent.putExtra(Constants.EXTRA_ROW_ID, id);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
                            }
                        });
                    }
                }
            });
        }

        if (!download.isStartFromContextMenu()) {
            Toast.makeText(getContext(), R.string.download_started, Toast.LENGTH_LONG)
                    .show();
        }
    }

    /*
     * show webview geolocation permission prompt
     */
    private void showGeolocationPermissionPrompt() {
        if (geolocationCallback == null) {
            return;
        }
        if (geoDialog != null && geoDialog.isShowing()) {
            return;
        }
        geoDialog = buildGeoPromptDialog();
        geoDialog.show();
    }

    public void dismissGeoDialog() {
        if (geoDialog != null) {
            geoDialog.dismiss();
            geoDialog = null;
        }
    }

    private AlertDialog buildGeoPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getString(R.string.geolocation_dialog_message, geolocationOrigin))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.geolocation_dialog_allow), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        acceptGeoRequest();
                    }
                })
                .setNegativeButton(getString(R.string.geolocation_dialog_block), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        rejectGeoRequest();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        rejectGeoRequest();
                    }
                });
        return builder.create();
    }

    private void acceptGeoRequest() {
        if (geolocationCallback == null) {
            return;
        }
        geolocationCallback.invoke(geolocationOrigin, true, false);
        geolocationOrigin = "";
        geolocationCallback = null;
    }

    private void rejectGeoRequest() {
        if (geolocationCallback == null) {
            return;
        }
        geolocationCallback.invoke(geolocationOrigin, false, false);
        geolocationOrigin = "";
        geolocationCallback = null;
    }

    private boolean isStartedFromExternalApp() {
        final Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        // No SafeIntent needed here because intent.getAction() is safe (SafeIntent simply calls intent.getAction()
        // without any wrapping):
        final Intent intent = activity.getIntent();
        boolean isFromInternal = intent != null && intent.getBooleanExtra(IntentUtils.EXTRA_IS_INTERNAL_REQUEST, false);
        return intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && !isFromInternal;
    }

    public boolean onBackPressed() {
        if (canGoBack()) {
            // Go back in web history
            goBack();
        } else {
            if (isStartedFromExternalApp()) {
                // We have been started from a VIEW intent. Go back to the previous app immediately (No erase).
                // However we need to finish the current session so that the custom tab config gets
                // correctly cleared:
                // FIXME: does Zerda need this?
                BrowsingSession.getInstance().clearCustomTabConfig();
                getActivity().finish();
            } else {
                // let parent to decide for this Fragment
                return false;
            }
        }

        return true;
    }

    public void setBlockingEnabled(boolean enabled) {
        final List<Tab> tabs = tabsSession.getTabs();
        for (final Tab tab : tabs) {
            tab.setBlockingEnabled(enabled);
        }
    }

    public void loadUrl(@NonNull final String url, boolean openNewTab) {
        updateURL(url);
        if (UrlUtils.isUrl(url)) {
            if (openNewTab) {
                tabsSession.addTab(url);
            } else {
                Tab currentTab = tabsSession.getCurrentTab();
                if (currentTab != null) {
                    tabsSession.getCurrentTab().getTabView().loadUrl(url);
                } else {
                    tabsSession.addTab(url);
                }
            }
        } else if (AppConstants.isDevBuild()) {
            // throw exception to highlight this issue, except release build.
            throw new RuntimeException("trying to open a invalid url: " + url);
        }
    }

    public void loadTab(final String tabId) {
        if (!TextUtils.isEmpty(tabId)) {
            tabsSession.switchToTab(tabId);
        }
    }

    public void openPreference() {
        notifyParent(FragmentListener.TYPE.OPEN_PREFERENCE, null);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.display_url:
                notifyParent(FragmentListener.TYPE.SHOW_URL_INPUT, getUrl());
                TelemetryWrapper.clickUrlbar();
                break;
            case R.id.btn_search:
                notifyParent(FragmentListener.TYPE.SHOW_URL_INPUT, getUrl());
                TelemetryWrapper.clickToolbarSearch();
                break;
            case R.id.btn_open_new_tab:
                notifyParent(FragmentListener.TYPE.SHOW_HOME, null);
                // FIXME: 2018/2/12 new telemetry for 2.0 UI
                break;
            case R.id.btn_tab_tray:
                notifyParent(FragmentListener.TYPE.SHOW_TAB_TRAY, null);
                TelemetryWrapper.showTabTrayToolbar();
                break;
            case R.id.btn_menu:
                notifyParent(FragmentListener.TYPE.SHOW_MENU, null);
                TelemetryWrapper.showMenuToolbar();
                break;
            case R.id.btn_capture:
                onCaptureClicked();
                // FIXME: 2/8/18 Is the telemetry supposed to share the v1 capture button?
                TelemetryWrapper.clickToolbarCapture();
                break;
            case R.id.customtab_close:
                BrowsingSession.getInstance().clearCustomTabConfig();
                getActivity().finishAndRemoveTask();
                break;
            default:
                throw new IllegalArgumentException("Unhandled menu item in BrowserFragment");
        }
    }

    private void notifyParent(FragmentListener.TYPE type, Object payload) {
        final Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, type, payload);
        }
    }

    @NonNull
    public String getUrl() {
        // getUrl() is used for things like sharing the current URL. We could try to use the webview,
        // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
        // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
        // user is probably expecting to share, so lets use that here:
        return urlView.getText().toString();
    }

    public boolean canGoForward() {
        return tabsSession.getCurrentTab() != null && tabsSession.getCurrentTab().getTabView().canGoForward();
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean canGoBack() {
        return tabsSession.getCurrentTab() != null && tabsSession.getCurrentTab().getTabView().canGoBack();
    }

    public void goBack() {
        final TabView current = tabsSession.getCurrentTab().getTabView();
        if (current != null) {
            WebBackForwardList webBackForwardList = ((WebView) current).copyBackForwardList();
            WebHistoryItem item = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() - 1);
            updateURL(item.getUrl());
            current.goBack();
        }
    }

    public void goForward() {
        final TabView current = tabsSession.getCurrentTab().getTabView();
        if (current != null) {
            WebBackForwardList webBackForwardList = ((WebView) current).copyBackForwardList();
            WebHistoryItem item = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() + 1);
            updateURL(item.getUrl());
            current.goForward();
        }
    }

    public void reload() {
        final TabView current = tabsSession.getCurrentTab().getTabView();
        if (current != null) {
            current.reload();
        }
    }

    public void stop() {
        final TabView current = tabsSession.getCurrentTab().getTabView();
        if (current != null) {
            current.stopLoading();
        }
    }

    public interface ScreenshotCallback {
        void onCaptureComplete(String title, String url, Bitmap bitmap);
    }

    public boolean capturePage(@NonNull ScreenshotCallback callback) {
        final TabView current = tabsSession.getCurrentTab().getTabView();
        // Failed to get WebView
        if (current == null || !(current instanceof WebView)) {
            return false;
        }
        WebView webView = (WebView) current;
        Bitmap content = getPageBitmap(webView);
        // Failed to capture
        if (content == null) {
            return false;
        }
        callback.onCaptureComplete(current.getTitle(), current.getUrl(), content);
        return true;
    }

    public void dismissWebContextMenu() {
        if (webContextMenu != null) {
            webContextMenu.dismiss();
            webContextMenu = null;
        }
    }

    private Bitmap getPageBitmap(WebView webView) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        try {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), (int) (webView.getContentHeight() * displaymetrics.density), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);
            return bitmap;
            // OOM may occur, even if OOMError is not thrown, operations during Bitmap creation may
            // throw other Exceptions such as NPE when the bitmap is very large.
        } catch (Exception | OutOfMemoryError ex) {
            return null;
        }

    }

    @Override
    public void onScreenshotTaken(String screenshotPath, String title) {
        if (screenshotObserver != null) {
            screenshotObserver.stop();
        }
        notifyParent(FragmentListener.TYPE.SHOW_SCREENSHOT_HINT, null);
    }

    class TabsContentListener implements TabsViewListener, TabsChromeListener {

        String failingUrl;
        // Some url may have two onPageFinished for the same url. filter them out to avoid
        // adding twice to the history.
        private String lastInsertedUrl = null;
        // Some url may report progress from 0 again for the same url. filter them out to avoid
        // progress bar regression when scrolling.
        private String loadedUrl = null;

        private ValueAnimator tabTransitionAnimator;

        @Override
        public void onTabHoist(@NonNull final Tab tab) {
            // ensure it does not have attach to parent earlier.
            tab.detach();

            @Nullable final View outView = findExistingTabView(webViewSlot);
            webViewSlot.removeView(outView);

            final View inView = tab.getTabView().getView();
            webViewSlot.addView(inView);

            startTabTransition(null, inView, null);
        }

        @Override
        public void onTabStarted(@NonNull Tab tab) {
            lastInsertedUrl = null;
            loadedUrl = null;

            updateIsLoading(true);

            siteIdentity.setImageLevel(SITE_GLOBE);

            backgroundTransition.resetTransition();
        }

        private void updateUrlFromWebView(@NonNull Tab source) {
            if (tabsSession.getCurrentTab() != null) {
                final String viewURL = tabsSession.getCurrentTab().getUrl();
                onURLChanged(source, viewURL);
            }
        }

        @Override
        public void onTabFinished(@NonNull Tab tab, boolean isSecure) {
            // The URL which is supplied in onTabFinished() could be fake (see #301), but webview's
            // URL is always correct _except_ for error pages
            updateUrlFromWebView(tab);

            updateIsLoading(false);

            notifyParent(FragmentListener.TYPE.UPDATE_MENU, null);

            backgroundTransition.startTransition(ANIMATION_DURATION);

            if (isSecure) {
                siteIdentity.setImageLevel(SITE_LOCK);
            }
            String urlToBeInserted = getUrl();
            if (!getUrl().equals(this.failingUrl) && !urlToBeInserted.equals(lastInsertedUrl)) {
                tabsSession.getCurrentTab().getTabView().insertBrowsingHistory();
                lastInsertedUrl = urlToBeInserted;
            }
        }

        @Override
        public void onTabCountChanged(int count) {
            tabCounter.setCountWithAnimation(count);
        }

        @Override
        public void onURLChanged(@NonNull Tab tab, final String url) {
            updateURL(url);
        }

        @Override
        public void onProgressChanged(@NonNull Tab tab, int progress) {
            if (tabsSession.getCurrentTab() != null) {
                final String currentUrl = tabsSession.getCurrentTab().getUrl();
                final boolean progressIsForLoadedUrl = TextUtils.equals(currentUrl, loadedUrl);
                // Some new url may give 100 directly and then start from 0 again. don't treat
                // as loaded for these urls;
                final boolean urlBarLoadingToFinished =
                        progressView.getMax() != progressView.getProgress() && progress == progressView.getMax();
                if (urlBarLoadingToFinished) {
                    loadedUrl = currentUrl;
                }
                if (progressIsForLoadedUrl) {
                    return;
                }
            }
            progressView.setProgress(progress);
        }

        @Override
        public boolean handleExternalUrl(final String url) {
            if (getContext() == null) {
                Log.w(FRAGMENT_TAG, "No context to use, abort callback handleExternalUrl");
                return false;
            }

            return tabsSession.getCurrentTab() != null
                    && IntentUtils.handleExternalUri(getContext(), tabsSession.getCurrentTab().getTabView(), url);
        }

        @Override
        public void onLongPress(@NonNull Tab tab, final TabView.HitTarget hitTarget) {
            if (getActivity() == null) {
                Log.w(FRAGMENT_TAG, "No context to use, abort callback onLongPress");
                return;
            }

            webContextMenu = WebContextMenu.show(getActivity(), downloadCallback, hitTarget);
        }

        @Override
        public void onEnterFullScreen(@NonNull Tab tab,
                                      @NonNull final TabView.FullscreenCallback callback,
                                      @Nullable View fullscreenContentView) {
            if (tabsSession.getCurrentTab() != tab) {
                callback.fullScreenExited();
                return;
            }

            fullscreenCallback = callback;

            if (tab.getTabView() != null && fullscreenContentView != null) {
                // Hide browser UI and web content
                browserContainer.setVisibility(View.INVISIBLE);

                // Add view to video container and make it visible
                final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                videoContainer.addView(fullscreenContentView, params);
                videoContainer.setVisibility(View.VISIBLE);

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = switchToImmersiveMode();
            }
        }

        @Override
        public void onExitFullScreen(@NonNull Tab tab) {
            // Remove custom video views and hide container
            videoContainer.removeAllViews();
            videoContainer.setVisibility(View.GONE);

            // Show browser UI and web content again
            browserContainer.setVisibility(View.VISIBLE);

            if (systemVisibility != NONE) {
                exitImmersiveMode(systemVisibility);
            }

            // Notify renderer that we left fullscreen mode.
            if (fullscreenCallback != null) {
                fullscreenCallback.fullScreenExited();
                fullscreenCallback = null;
            }

            // WebView gets focus, but unable to open the keyboard after exit Fullscreen for Android 7.0+
            // We guess some component in WebView might lock focus
            // So when user touches the input text box on Webview, it will not trigger to open the keyboard
            // It may be a WebView bug.
            // The workaround is clearing WebView focus
            // The WebView will be normal when it gets focus again.
            // If android change behavior after, can remove this.
            if (tabsSession.getCurrentTab().getTabView() instanceof WebView) {
                ((WebView) tabsSession.getCurrentTab().getTabView()).clearFocus();
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Tab tab,
                                                       final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            geolocationOrigin = origin;
            geolocationCallback = callback;
            permissionHandler.tryAction(BrowserFragment.this, Manifest.permission.ACCESS_FINE_LOCATION, ACTION_GEO_LOCATION, null);
        }

        @Override
        public boolean onShowFileChooser(@NonNull Tab tab,
                                         WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams) {
            TelemetryWrapper.browseFilePermissionEvent();
            try {
                BrowserFragment.this.fileChooseAction = new FileChooseAction(BrowserFragment.this, filePathCallback, fileChooserParams);
                permissionHandler.tryAction(BrowserFragment.this, Manifest.permission.READ_EXTERNAL_STORAGE, BrowserFragment.ACTION_PICK_FILE, null);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void updateFailingUrl(@NonNull Tab tab, String url, boolean updateFromError) {
            if (!updateFromError && !url.equals(failingUrl)) {
                failingUrl = null;
            } else {
                this.failingUrl = url;
            }
        }

        @Override
        public void onReceivedTitle(@NonNull Tab tab, String title) {
            if (!BrowserFragment.this.getUrl().equals(tab.getUrl())) {
                updateURL(tab.getUrl());
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void startTabTransition(@Nullable final View outView, @NonNull final View inView,
                                        @Nullable final Runnable finishCallback) {
            stopTabTransition();

            inView.setAlpha(0f);
            if (outView != null) {
                outView.setAlpha(1f);
            }

            int duration = inView.getResources().getInteger(R.integer.tab_transition_time);
            tabTransitionAnimator = ValueAnimator.ofFloat(0, 1).setDuration(duration);
            tabTransitionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float alpha = (float) animation.getAnimatedValue();
                    if (outView != null) {
                        outView.setAlpha(1 - alpha);
                    }
                    inView.setAlpha(alpha);
                }
            });
            tabTransitionAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (finishCallback != null) {
                        finishCallback.run();
                    }
                    inView.setAlpha(1f);
                    if (outView != null) {
                        outView.setAlpha(1f);
                    }
                }
            });
            tabTransitionAnimator.start();
        }

        @Nullable
        private View findExistingTabView(ViewGroup parent) {
            int viewCount = parent.getChildCount();
            for (int childIdx = 0; childIdx < viewCount; ++childIdx) {
                View childView = parent.getChildAt(childIdx);
                if (childView instanceof TabView) {
                    return ((TabView) childView).getView();
                }
            }
            return null;
        }

        private void stopTabTransition() {
            if (tabTransitionAnimator != null && tabTransitionAnimator.isRunning()) {
                tabTransitionAnimator.end();
            }
        }
    }

    class DownloadCallback implements org.mozilla.focus.web.DownloadCallback {

        @Override
        public void onDownloadStart(@NonNull Download download) {
            permissionHandler.tryAction(BrowserFragment.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_DOWNLOAD, download);
        }
    }
}
