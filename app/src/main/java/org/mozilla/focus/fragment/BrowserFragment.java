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
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.menu.WebContextMenu;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.ColorUtils;
import org.mozilla.focus.utils.DrawableUtils;
import org.mozilla.focus.utils.FilePickerUtil;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.MimeUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.CustomTabConfig;
import org.mozilla.focus.web.Download;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.widget.AnimatedProgressBar;
import org.mozilla.focus.widget.BackKeyHandleable;
import org.mozilla.focus.widget.FragmentListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Fragment for displaying the browser UI.
 */
public class BrowserFragment extends WebFragment implements View.OnClickListener, BackKeyHandleable {

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

    public static BrowserFragment create(String url) {
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

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        updateURL(getInitialUrl());

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
                    TelemetryWrapper.customTabActionButtonEvent();
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
            private final static int NONE = -1;
            private int systemVisibility = NONE;

            @Override
            public void onPageStarted(final String url) {
                updateIsLoading(true);

                siteIdentity.setImageLevel(SITE_GLOBE);

                progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading));


                backgroundTransition.resetTransition();

                progressView.setProgress(5);
                progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(boolean isSecure) {
                updateIsLoading(false);

                backgroundTransition.startTransition(ANIMATION_DURATION);

                progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading_finished));

                progressView.setProgress(progressView.getMax());
                progressView.setVisibility(View.GONE);

                if (isSecure) {
                    siteIdentity.setImageLevel(SITE_LOCK);
                }
            }

            @Override
            public void onURLChanged(final String url) {
                updateURL(url);
            }

            @Override
            public void onProgress(int progress) {
                progressView.setProgress(progress);
                if (progress == progressView.getMax()) {
                    progressView.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean handleExternalUrl(final String url) {
                final IWebView webView = getWebView();

                return webView != null && IntentUtils.handleExternalUri(getContext(), webView, url);
            }

            @Override
            public void onLongPress(final IWebView.HitTarget hitTarget) {
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
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // We do have the permission to write to the external storage. Proceed with the download.
                    queueDownload(download);
                } else {
                    // We do not have the permission to write to the external storage. Request the permission and start the
                    // download from onRequestPermissionsResult().
                    final Activity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

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
                try {
                    BrowserFragment.this.fileChooseAction = new FileChooseAction(filePathCallback, fileChooserParams);
                    BrowserFragment.this.fileChooseAction.performAction();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
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

        final String dir = MimeUtils.isImage(download.getMimeType())
                ? Environment.DIRECTORY_PICTURES
                : Environment.DIRECTORY_DOWNLOADS;
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download.getUrl()))
                .addRequestHeader("User-Agent", download.getUserAgent())
                .addRequestHeader("Cookie", cookie)
                .addRequestHeader("Referer", getUrl())
                .setDestinationInExternalPublicDir(dir, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType(download.getMimeType());

        request.allowScanningByMediaScanner();

        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Long downloadId = manager.enqueue(request);

        //record download ID
        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setDownloadId(downloadId);
        downloadInfo.setFileName(fileName);
        DownloadInfoManager.getInstance().insert(downloadInfo, null);

        Snackbar.make(browserContainer, R.string.download_started, Snackbar.LENGTH_LONG)
                .show();
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

                TelemetryWrapper.eraseBackEvent();
            } else {
                // let parent to decide for this Fragment
                return false;
            }
        }

        return true;
    }

    // This is not used currently cause we remove most erasing entry point. We'll need this later.
    public void erase() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.cleanup();
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
                break;
            case R.id.btn_search:
                notifyParent(FragmentListener.TYPE.SHOW_URL_INPUT, getUrl());
                break;
            case R.id.btn_home:
                notifyParent(FragmentListener.TYPE.SHOW_HOME, null);
                break;
            case R.id.btn_menu:
                notifyParent(FragmentListener.TYPE.SHOW_MENU, null);
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
            webView.goBack();
        }
    }

    public void goForward() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.goForward();
        }
    }

    public void loadUrl(final String url) {
        final IWebView webView = getWebView();
        if (webView != null) {
            super.pendingUrl = null;
            webView.loadUrl(url);
        } else {
            super.pendingUrl = url;
        }
    }

    public void reload() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.reload();
        }
    }

    public boolean capturePage() {
        final IWebView iwebView = getWebView();
        if (iwebView != null && iwebView instanceof WebView) {
            WebView webView = (WebView) iwebView;
            try {
                Bitmap content = getPageBitmap(webView);
                if (content != null) {
                    saveBitmapToStorage(webView.getTitle() + "." + Calendar.getInstance().getTimeInMillis(), content);
                    return true;
                } else {
                    return false;
                }

            } catch (IOException ex) {
                return false;
            }
        }
        return false;
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

    private void saveBitmapToStorage(String fileName, Bitmap bitmap) throws IOException {
        File folder = new File(Environment.getExternalStorageDirectory(), "Zerda");
        if (!folder.exists() && !folder.mkdir()) {
            throw new IOException("Can't create folder");
        }
        fileName = fileName.concat(".png");
        File file = new File(folder, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            notifyNewScreenshot(file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyNewScreenshot(String path) {
        MediaScannerConnection.scanFile(getContext(), new String[]{path}, new String[]{null}, null);
    }

    public void setBlockingEnabled(boolean enabled) {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.setBlockingEnabled(enabled);
        }

        backgroundView.setBackgroundResource(enabled ? R.drawable.animated_background : R.drawable.animated_background_disabled);
        backgroundTransition = (TransitionDrawable) backgroundView.getBackground();
    }

    public boolean isBlockingEnabled() {
        final IWebView webView = getWebView();
        return webView == null || webView.isBlockingEnabled();
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
}
