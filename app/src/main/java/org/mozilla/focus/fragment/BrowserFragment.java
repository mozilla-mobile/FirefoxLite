/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.Manifest;
import android.app.Activity;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import org.mozilla.focus.menu.WebContextMenu;
import org.mozilla.focus.screenshot.ScreenshotObserver;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.ColorUtils;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.DrawableUtils;
import org.mozilla.focus.utils.FilePickerUtil;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.CustomTabConfig;
import org.mozilla.focus.web.Download;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.widget.AnimatedProgressBar;
import org.mozilla.focus.widget.BackKeyHandleable;
import org.mozilla.focus.widget.FragmentListener;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Fragment for displaying the browser UI.
 */
public class BrowserFragment extends WebFragment implements View.OnClickListener, BackKeyHandleable, ScreenshotObserver.OnScreenshotListener {

    public static final String FRAGMENT_TAG = "browser";

    private static int REQUEST_CODE_READ_STORAGE_PERMISSION = 100;
    private static int REQUEST_CODE_WRITE_STORAGE_PERMISSION = 101;
    private static int REQUEST_CODE_LOCATION_PERMISSION = 102;
    private static int REQUEST_CODE_CHOOSE_FILE = 103;

    private static final int ANIMATION_DURATION = 300;
    private static final String ARGUMENT_URL = "url";
    private static final String RESTORE_KEY_DOWNLOAD = "download";

    private static final int SITE_GLOBE = 0;
    private static final int SITE_LOCK = 1;

    private String firstLoadingUrlAfterResumed = null;

    public static BrowserFragment create(@NonNull String url) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);

        BrowserFragment fragment = new BrowserFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private Download pendingDownload;
    private View backgroundView;
    private TransitionDrawable backgroundTransition;
    private TextView urlView;
    private AnimatedProgressBar progressView;
    private ImageView siteIdentity;

    //GeoLocationPermission
    private String geolocationOrigin;
    private GeolocationPermissions.Callback geolocationCallback;

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private ViewGroup videoContainer;

    /**
     * Container containing the browser chrome and web content.
     */
    private View browserContainer;

    private IWebView.FullscreenCallback fullscreenCallback;

    private boolean isLoading = false;

    // Set an initial WeakReference so we never have to handle loadStateListenerWeakReference being null
    // (i.e. so we can always just .get()).
    private WeakReference<LoadStateListener> loadStateListenerWeakReference = new WeakReference<>(null);

    // pending action for file-choosing
    private FileChooseAction fileChooseAction;

    private ScreenshotObserver screenshotObserver;

    @Override
    public void onPause() {
        super.onPause();
        if(screenshotObserver != null) {
            screenshotObserver.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(Settings.getInstance(getActivity()).shouldShowScreenshotOnBoarding()) {
            screenshotObserver = new ScreenshotObserver(getActivity(), this);
            screenshotObserver.start();
        }
    }

    @Override
    public String getInitialUrl() {
        return getArguments().getString(ARGUMENT_URL);
    }

    private void updateURL(final String url) {
        urlView.setText(UrlUtils.stripUserInfo(url));
    }

    @Override
    public View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(RESTORE_KEY_DOWNLOAD)) {
            // If this activity was destroyed before we could start a download (e.g. because we were waiting for a permission)
            // then restore the download object.
            pendingDownload = savedInstanceState.getParcelable(RESTORE_KEY_DOWNLOAD);
        }

        final View view = inflater.inflate(R.layout.fragment_browser, container, false);

        videoContainer = (ViewGroup) view.findViewById(R.id.video_container);
        browserContainer = view.findViewById(R.id.browser_container);

        urlView = (TextView) view.findViewById(R.id.display_url);

        backgroundView = view.findViewById(R.id.background);
        backgroundTransition = (TransitionDrawable) backgroundView.getBackground();

        final View searchBtn = view.findViewById(R.id.btn_search);
        final View homeBtn = view.findViewById(R.id.btn_home);
        final View menuBtn = view.findViewById(R.id.btn_menu);
        if (searchBtn != null) {
            searchBtn.setOnClickListener(this);
        }
        if (homeBtn != null) {
            homeBtn.setOnClickListener(this);
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

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CHOOSE_FILE) {
            final boolean done = (fileChooseAction == null) || fileChooseAction.onFileChose(resultCode, data);
            if (done) {
                fileChooseAction = null;
            }
        }
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
        super.onSaveInstanceState(outState);

        if (pendingDownload != null) {
            // We were not able to start this download yet (waiting for a permission). Save this download
            // so that we can start it once we get restored and receive the permission.
            outState.putParcelable(RESTORE_KEY_DOWNLOAD, pendingDownload);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        notifyParent(FragmentListener.TYPE.FRAGMENT_STARTED, FRAGMENT_TAG);
    }

    @Override
    public void onStop() {
        super.onStop();
        notifyParent(FragmentListener.TYPE.FRAGMENT_STOPPED, FRAGMENT_TAG);
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

    private void updateIsLoading(final boolean isLoading) {
        this.isLoading = isLoading;
        final LoadStateListener currentListener = loadStateListenerWeakReference.get();
        if (currentListener != null) {
            currentListener.isLoadingChanged(isLoading);
        }
    }

    @Override
    public IWebView.Callback createCallback() {
        return new IWebView.Callback() {
            String failingUrl;
            private final static int NONE = -1;
            private int systemVisibility = NONE;
            private boolean mostOldCallbacksHaveFinished = false;
            // Some url may have two onPageFinished for the same url. filter them out to avoid
            // adding twice to the history.
            private String lastInsertedUrl = null;
            // Some url may report progress from 0 again for the same url. filter them out to avoid
            // progress bar regression when scrolling.
            private String loadedUrl = null;

            @Override
            public void onPageStarted(final String url) {
                // This mostOldCallbacksHaveFinished flag sort of works like onPageCommitVisible.
                // There are some callback triggers that are fired due to Webview.restoreState()
                // or last webview loading that are not finished. As a quick fix we filtered these
                // onPageFinished and onProgress out by only consuming the callbacks after our
                // targeted url has fired a onPageStarted. This is assuming we will always have a
                // such onPageStarted call back after webview.loadUrl() which I am not certain is
                // guaranteed. We filtered out these since they are having some properties such as:
                // the getTitle() returned here is incomplete. This is most likely the
                // onPageFinished events that are fired by didFinishNavigation
                // See: https://stackoverflow.com/a/46298285/3591480
                if (firstLoadingUrlAfterResumed != null && UrlUtils.urlsMatchExceptForTrailingSlash(firstLoadingUrlAfterResumed, url)) {
                    mostOldCallbacksHaveFinished = true;
                }
                lastInsertedUrl = null;
                loadedUrl = null;

                updateIsLoading(true);

                siteIdentity.setImageLevel(SITE_GLOBE);

                backgroundTransition.resetTransition();
            }

            private void updateUrlFromWebView() {
                final IWebView webView = getWebView();
                if(webView != null) {
                    final String viewURL = webView.getUrl();
                    if (!UrlUtils.isInternalErrorURL(viewURL)) {
                        onURLChanged(viewURL);
                    }
                }
            }

            @Override
            public void onPageFinished(boolean isSecure) {
                if(!mostOldCallbacksHaveFinished) {
                    return;
                }
                // The URL which is supplied in onPageFinished() could be fake (see #301), but webview's
                // URL is always correct _except_ for error pages
                updateUrlFromWebView();

                updateIsLoading(false);

                notifyParent(FragmentListener.TYPE.UPDATE_MENU, null);

                backgroundTransition.startTransition(ANIMATION_DURATION);

                if (isSecure) {
                    siteIdentity.setImageLevel(SITE_LOCK);
                }
                String urlToBeInserted = getUrl();
                if (!getUrl().equals(this.failingUrl) && !urlToBeInserted.equals(lastInsertedUrl) && getWebView()!=null) {
                    getWebView().insertBrowsingHistory();
                    lastInsertedUrl = urlToBeInserted;
                }
            }

            @Override
            public void onURLChanged(final String url) {
                updateURL(url);
            }

            @Override
            public void onProgress(int progress) {
                if(!mostOldCallbacksHaveFinished) {
                    return;
                }
                final IWebView webView = getWebView();
                if (webView != null) {
                    final String currentUrl = webView.getUrl();
                    final boolean progressIsForLoadedUrl = currentUrl.equals(loadedUrl);
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
                final IWebView webView = getWebView();
                if (getContext() == null) {
                    Log.w(FRAGMENT_TAG, "No context to use, abort callback handleExternalUrl");
                    return false;
                }

                return webView != null && IntentUtils.handleExternalUri(getContext(), webView, url);
            }

            @Override
            public void onLongPress(final IWebView.HitTarget hitTarget) {
                if (getActivity() == null) {
                    Log.w(FRAGMENT_TAG, "No context to use, abort callback onLongPress");
                    return;
                }

                WebContextMenu.show(getActivity(), this, hitTarget);
            }

            @Override
            public void onEnterFullScreen(@NonNull final IWebView.FullscreenCallback callback, @Nullable View view) {
                fullscreenCallback = callback;

                if (view != null) {
                    // Hide browser UI and web content
                    browserContainer.setVisibility(View.INVISIBLE);

                    // Add view to video container and make it visible
                    final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    videoContainer.addView(view, params);
                    videoContainer.setVisibility(View.VISIBLE);

                    // Switch to immersive mode: Hide system bars other UI controls
                    systemVisibility = switchToImmersiveMode();
                }
            }

            @Override
            public void onExitFullScreen() {
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
            }

            @Override
            public void onDownloadStart(Download download) {
                if (getContext() == null) {
                    Log.w(FRAGMENT_TAG, "No context to use, abort callback onDownloadStart");
                    return;
                }

                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // We do have the permission to write to the external storage. Proceed with the download.
                    queueDownload(download);
                } else {
                    // We do not have the permission to write to the external storage. Request the permission and start the
                    // download from onRequestPermissionsResult().
                    pendingDownload = download;
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_STORAGE_PERMISSION);
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
                geolocationOrigin = origin;
                geolocationCallback = callback;
                //show geolocation permission prompt
                showGeolocationPermissionPrompt(geolocationOrigin, geolocationCallback);
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                TelemetryWrapper.browseFilePermissionEvent();
                try {
                    BrowserFragment.this.fileChooseAction = new FileChooseAction(filePathCallback, fileChooserParams);
                    BrowserFragment.this.fileChooseAction.performAction();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public void updateFailingUrl(String url, boolean updateFromError) {
                if( !updateFromError && !url.equals(failingUrl)) {
                    failingUrl = null;
                } else {
                    this.failingUrl = url;
                }
            }
        };
    }

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    private int switchToImmersiveMode() {
        final Activity activity = getActivity();
        final int original = activity.getWindow().getDecorView().getSystemUiVisibility();
        activity.getWindow().getDecorView().setSystemUiVisibility(
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

        activity.getWindow().getDecorView().setSystemUiVisibility(visibility);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_WRITE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                queueDownload(pendingDownload);
            }

            pendingDownload = null;
            return;
        } else if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                geolocationCallback.invoke(geolocationOrigin, true, false);
            } else {
                geolocationCallback.invoke(geolocationOrigin, false, false);
            }
            geolocationOrigin = "";
            geolocationCallback = null;
            return;
        } else if (requestCode == REQUEST_CODE_READ_STORAGE_PERMISSION) {
            if (fileChooseAction != null) {
                final boolean granted = (grantResults.length > 0)
                        && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if (granted) {
                    fileChooseAction.onPermissionGranted();
                } else {
                    fileChooseAction.cancel();
                    fileChooseAction = null;
                }
            }

        }

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
        if(!URLUtil.isNetworkUrl(download.getUrl())) {
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

        //record download ID
        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setDownloadId(downloadId);
        // When downloading downloaded content, DownloadManager returns the previous download id
        // Do not insert again if we're already showing it.
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
                    if(!downloadInfoList.isEmpty()) {
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
    private void showGeolocationPermissionPrompt(final String origin, final GeolocationPermissions.Callback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getString(R.string.geolocation_dialog_message, origin))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.geolocation_dialog_allow), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //check location permission
                        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                            callback.invoke(origin, true, false);
                            geolocationOrigin = "";
                            geolocationCallback = null;
                        } else {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
                        }
                    }
                }).setNegativeButton(getString(R.string.geolocation_dialog_block), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                callback.invoke(origin, false, false);
                geolocationOrigin = "";
                geolocationCallback = null;
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isStartedFromExternalApp() {
        final Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        // No SafeIntent needed here because intent.getAction() is safe (SafeIntent simply calls intent.getAction()
        // without any wrapping):
        final Intent intent = activity.getIntent();
        return intent != null && Intent.ACTION_VIEW.equals(intent.getAction());
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
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.setBlockingEnabled(enabled);
        }
    }

    // This is not used currently cause we remove most erasing entry point. We'll need this later.
    public void erase() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.cleanup();
        }
    }

    @Override
    public void loadUrl(@NonNull final String url) {
        firstLoadingUrlAfterResumed = url;
        updateURL(url);
        super.loadUrl(url);
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
            case R.id.btn_home:
                notifyParent(FragmentListener.TYPE.SHOW_HOME, null);
                TelemetryWrapper.clickToolbarHome();
                break;
            case R.id.btn_menu:
                notifyParent(FragmentListener.TYPE.SHOW_MENU, null);
                TelemetryWrapper.showMenuToolbar();
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
        final IWebView webView = getWebView();
        return webView != null && webView.canGoForward();
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean canGoBack() {
        final IWebView webView = getWebView();
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        final IWebView webView = getWebView();
        if (webView != null) {
            WebBackForwardList webBackForwardList = ((WebView) webView).copyBackForwardList();
            WebHistoryItem item = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() - 1);
            updateURL(item.getUrl());
            webView.goBack();
        }
    }

    public void goForward() {
        final IWebView webView = getWebView();
        if (webView != null) {
            WebBackForwardList webBackForwardList = ((WebView) webView).copyBackForwardList();
            WebHistoryItem item = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() + 1);
            updateURL(item.getUrl());
            webView.goForward();
        }
    }

    public void reload() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.reload();
        }
    }

    public void stop() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.stopLoading();
        }
    }

    public interface ScreenshotCallback {
        void onCaptureComplete(String title, String url, Bitmap bitmap);
    }

    public boolean capturePage(@NonNull ScreenshotCallback callback) {
        final IWebView iwebView = getWebView();
        // Failed to get Webview
        if (iwebView == null || !(iwebView instanceof WebView)) {
            return false;
        }
        WebView webView = (WebView) iwebView;
        Bitmap content = getPageBitmap(webView);
        // Failed to capture
        if (content == null) {
            return false;
        }
        callback.onCaptureComplete(iwebView.getTitle(), iwebView.getUrl(), content);
        return true;
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

    class FileChooseAction {
        private ValueCallback<Uri[]> callback;
        private WebChromeClient.FileChooserParams params;
        private Uri[] uris;

        FileChooseAction(@NonNull ValueCallback<Uri[]> callback,
                         @NonNull WebChromeClient.FileChooserParams params) {
            this.callback = callback;
            this.params = params;
        }

        public void performAction() {
            // check permission before we pick any file.
            final int permission = ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            if (PackageManager.PERMISSION_GRANTED == permission) {
                startChooserActivity();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_READ_STORAGE_PERMISSION);
            }
        }

        public void onPermissionGranted() {
            startChooserActivity();
        }

        public void cancel() {
            this.callback.onReceiveValue(null);
        }

        /**
         * Callback when back from a File-choose-activity
         *
         * @param resultCode
         * @param data
         * @return true if this action is done
         */
        public boolean onFileChose(int resultCode, Intent data) {
            if (this.callback == null) {
                return true;
            }

            if ((resultCode != Activity.RESULT_OK) || (data == null)) {
                this.callback.onReceiveValue(null);
                this.callback = null;
                return true;
            }

            try {
                final Uri uri = data.getData();
                uris = (uri == null) ? null : new Uri[]{uri};

                // FIXME: check permission before access the uri
                // if file locates on external storage and we haven't granted permission
                // we might get exception here. but try won't work here.
                this.callback.onReceiveValue(uris);
            } catch (Exception e) {
                this.callback.onReceiveValue(null);
                e.printStackTrace();
            }

            this.callback = null;
            return true;
        }

        private void startChooserActivity() {
            startActivityForResult(createChooserIntent(this.params), REQUEST_CODE_CHOOSE_FILE);
        }

        private Intent createChooserIntent(WebChromeClient.FileChooserParams params) {
            final String[] mimeTypes = params.getAcceptTypes();
            CharSequence title = params.getTitle();
            title = TextUtils.isEmpty(title) ? getString(R.string.file_picker_title) : title;
            return FilePickerUtil.getFilePickerIntent(getActivity(), title, mimeTypes);
        }
    }

    @Override
    public void onScreenshotTaken(String screenshotPath, String title) {
        if(screenshotObserver != null) {
            screenshotObserver.stop();
        }
        notifyParent(FragmentListener.TYPE.SHOW_SCREENSHOT_HINT, null);
    }
}
