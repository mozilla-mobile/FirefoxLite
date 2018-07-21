/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.download.GetImgHeaderTask;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.rocket.tabs.Tab;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabsSession;
import org.mozilla.rocket.tabs.TabsSessionProvider;
import org.mozilla.rocket.tabs.utils.TabUtil;
import org.mozilla.rocket.tabs.web.Download;
import org.mozilla.rocket.tabs.web.DownloadCallback;

import java.util.List;

public class WebContextMenu {
    public static final String DEFAULT_DOWNLOAD_EXTENSION = ".bin";

    private static View createTitleView(final @NonNull Context context, final @NonNull String title) {
        final TextView titleView = (TextView) LayoutInflater.from(context).inflate(R.layout.context_menu_title, (ViewGroup) null);
        titleView.setText(title);
        return titleView;
    }

    public static Dialog show(final boolean inPrivate,
                              final @NonNull Activity activity,
                              final @NonNull DownloadCallback callback,
                              final @NonNull TabView.HitTarget hitTarget) {

        if (!(hitTarget.isLink || hitTarget.isImage)) {
            // We don't support any other classes yet:
            throw new IllegalStateException("WebContextMenu can only handle long-press on images and/or links.");
        }

        TelemetryWrapper.openWebContextMenuEvent();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final View titleView;
        if (hitTarget.isLink) {
            titleView = createTitleView(activity, hitTarget.linkURL);
        } else if (hitTarget.isImage) {
            titleView = createTitleView(activity, hitTarget.imageURL);
        } else {
            throw new IllegalStateException("Unhandled long press target type");
        }
        builder.setCustomTitle(titleView);

        final View view = LayoutInflater.from(activity).inflate(R.layout.context_menu, (ViewGroup) null);
        builder.setView(view);

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // This even is only sent when the back button is pressed, or when a user
                // taps outside of the dialog:
                TelemetryWrapper.cancelWebContextMenuEvent();
            }
        });

        final Dialog dialog = builder.create();
        dialog.setOwnerActivity(activity);

        final NavigationView menu = view.findViewById(R.id.context_menu);
        setupMenuForHitTarget(inPrivate, dialog, menu, callback, hitTarget);

        dialog.show();
        return dialog;
    }

    /**
     * Set up the correct menu contents. Note: this method can only be called once the Dialog
     * has already been created - we need the dialog in order to be able to dismiss it in the
     * menu callbacks.
     */
    private static void setupMenuForHitTarget(final @NonNull boolean inPrivate,
                                              final @NonNull Dialog dialog,
                                              final @NonNull NavigationView navigationView,
                                              final @NonNull DownloadCallback callback,
                                              final @NonNull TabView.HitTarget hitTarget) {
        navigationView.inflateMenu(R.menu.menu_browser_context);

        final String targetUrl = hitTarget.isLink ? hitTarget.linkURL : hitTarget.imageURL;
        boolean canOpenInNewTab = canOpenInNewTab(dialog.getOwnerActivity(), targetUrl);

        // so far, Private Mode does not support multiple tabs
        canOpenInNewTab = canOpenInNewTab && !inPrivate;

        navigationView.getMenu().findItem(R.id.menu_new_tab).setVisible(canOpenInNewTab && hitTarget.isLink);
        navigationView.getMenu().findItem(R.id.menu_new_tab_image).setVisible(canOpenInNewTab && !hitTarget.isLink && hitTarget.isImage);

        navigationView.getMenu().findItem(R.id.menu_link_share).setVisible(hitTarget.isLink);
        navigationView.getMenu().findItem(R.id.menu_link_copy).setVisible(hitTarget.isLink);
        navigationView.getMenu().findItem(R.id.menu_image_share).setVisible(hitTarget.isImage);
        navigationView.getMenu().findItem(R.id.menu_image_copy).setVisible(hitTarget.isImage);

        navigationView.getMenu().findItem(R.id.menu_image_save).setVisible(
                hitTarget.isImage && UrlUtils.isHttpOrHttps(hitTarget.imageURL));

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                dialog.dismiss();

                switch (item.getItemId()) {
                    case R.id.menu_new_tab:
                    case R.id.menu_new_tab_image:
                        openInNewTab(hitTarget.source, dialog, targetUrl);
                        return true;
                    case R.id.menu_link_share: {
                        TelemetryWrapper.shareLinkEvent();
                        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, hitTarget.linkURL);
                        dialog.getContext().startActivity(Intent.createChooser(shareIntent, dialog.getContext().getString(R.string.share_dialog_title)));
                        return true;
                    }
                    case R.id.menu_image_share: {
                        TelemetryWrapper.shareImageEvent();
                        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, hitTarget.imageURL);
                        dialog.getContext().startActivity(Intent.createChooser(shareIntent, dialog.getContext().getString(R.string.share_dialog_title)));
                        return true;
                    }
                    case R.id.menu_image_save: {
                        if (URLUtil.guessFileName(hitTarget.imageURL, null, null).endsWith(DEFAULT_DOWNLOAD_EXTENSION)) {
                            GetImgHeaderTask getImgHeaderTask = new GetImgHeaderTask();
                            getImgHeaderTask.setCallback(new GetImgHeaderTask.Callback() {
                                @Override
                                public void setMIMEType(String mimeType) {
                                    final Download download = new Download(hitTarget.imageURL, null, null, mimeType, -1, true);
                                    callback.onDownloadStart(download);
                                }
                            });

                            getImgHeaderTask.execute(hitTarget.imageURL);
                        } else {
                            final Download download = new Download(hitTarget.imageURL, null, null, null, -1, true);
                            callback.onDownloadStart(download);
                        }

                        TelemetryWrapper.saveImageEvent();
                        return true;
                    }
                    case R.id.menu_link_copy:
                    case R.id.menu_image_copy:
                        final ClipboardManager clipboard = (ClipboardManager)
                                dialog.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        final Uri uri;

                        if (item.getItemId() == R.id.menu_link_copy) {
                            TelemetryWrapper.copyLinkEvent();
                            uri = Uri.parse(hitTarget.linkURL);
                        } else if (item.getItemId() == R.id.menu_image_copy) {
                            TelemetryWrapper.copyImageEvent();
                            uri = Uri.parse(hitTarget.imageURL);
                        } else {
                            throw new IllegalStateException("Unknown hitTarget type - cannot copy to clipboard");
                        }

                        final ClipData clip = ClipData.newUri(dialog.getContext().getContentResolver(), "URI", uri);
                        clipboard.setPrimaryClip(clip);
                        return true;
                    default:
                        throw new IllegalArgumentException("Unhandled menu item id=" + item.getItemId());
                }
            }
        });
    }

    private static boolean canOpenInNewTab(Activity activity, String url) {
        return activity != null &&
                !TextUtils.isEmpty(url) &&
                TabsSessionProvider.getOrNull(activity) != null;
    }

    private static void openInNewTab(final TabView source, final Dialog dialog, final String url) {
        final TabsSession session = TabsSessionProvider.getOrThrow(dialog.getOwnerActivity());
        final List<Tab> tabs = session.getTabs();

        String parentId = null;

        // Try to find parent tab for new tab
        for (final Tab tab : tabs) {
            if (tab.getTabView() == source) {
                parentId = tab.getId();
                break;
            }
        }

        Bundle args = TabUtil.argument(parentId, false, false);
        args.putInt(BrowserFragment.EXTRA_NEW_TAB_SRC, BrowserFragment.SRC_CONTEXT_MENU);
        session.addTab(url, args);
        TelemetryWrapper.addNewTabFromContextMenu();
    }
}
