/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.components.ComponentToggleService;
import org.mozilla.focus.utils.Browsers;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;

@TargetApi(Build.VERSION_CODES.N)
public class DefaultBrowserPreference extends Preference {

    public final static String EXTRA_RESOLVE_BROWSER = "_intent_to_resolve_browser_";

    private Switch switchView;

    private DefaultBrowserAction action;

    @SuppressWarnings("unused") // Instantiated from XML
    public DefaultBrowserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.preference_default_browser);
        init();
    }

    @SuppressWarnings("unused") // Instantiated from XML
    public DefaultBrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_default_browser);
        init();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        switchView = (Switch) view.findViewById(R.id.switch_widget);
        update();
    }

    public void update() {
        if (switchView != null) {
            final boolean isDefaultBrowser = Browsers.isDefaultBrowser(getContext());

            switchView.setChecked(isDefaultBrowser);
            if (ComponentToggleService.isAlive(getContext())) {
                setEnabled(false);
                setSummary(R.string.preference_default_browser_is_setting);
            } else {
                setEnabled(true);
                setSummary(null);
            }

            Settings.updatePrefDefaultBrowserIfNeeded(getContext(), isDefaultBrowser);
        }
    }

    @Override
    protected void onClick() {
        action.onPrefClicked();
    }

    public void onFragmentResume() {
        this.update();
        action.onFragmentResume();
    }

    public void onFragmentPause() {
        action.onFragmentPause();
    }

    private void init() {
        if (action == null) {
            action = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    ? new DefaultAction(this)
                    : new LowSdkAction(this);

        }
    }

    private void openAppDetailSettings(Context context) {
        //  TODO: extract this to util module
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        //  fromParts might be faster than parse: ex. Uri.parse("package://"+context.getPackageName());
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    private void openDefaultAppsSettings(Context context) {
        //  TODO: extract this to util module, return false to allow caller to handle
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // In some cases, a matching Activity may not exist (according to the Android docs).
            openSumoPage(context);
        }
    }

    private void clearDefaultBrowser(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, ComponentToggleService.class));
        context.startService(intent);
    }

    private void openSumoPage(Context context) {
        final Intent intent = InfoActivity.getIntentFor(context, SupportUtils.getSumoURLForTopic(context, "rocket-default"), getTitle().toString());
        context.startActivity(intent);
    }

    private void triggerWebOpen() {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setData(Uri.parse("http://mozilla.org"));

        //  Put a mojo to force MainActivity finish it's self, we probably need an intent flag to handle the task problem (reorder/parent/top)
        viewIntent.putExtra(EXTRA_RESOLVE_BROWSER, true);
        getContext().startActivity(viewIntent);
    }

    /**
     * To define necessary actions for setting default-browser.
     */
    private interface DefaultBrowserAction {
        void onPrefClicked();

        void onFragmentResume();

        void onFragmentPause();
    }

    private static class DefaultAction implements DefaultBrowserAction {
        DefaultBrowserPreference pref;

        DefaultAction(@NonNull final DefaultBrowserPreference pref) {
            this.pref = pref;
        }

        public void onPrefClicked() {
            // fire an intent and start related activity immediately
            pref.openDefaultAppsSettings(pref.getContext());
        }

        public void onFragmentResume() {

        }

        public void onFragmentPause() {
        }
    }

    /**
     * For android sdk version older than N
     */
    private static class LowSdkAction implements DefaultBrowserAction {
        DefaultBrowserPreference pref;
        BroadcastReceiver receiver;

        LowSdkAction(@NonNull final DefaultBrowserPreference pref) {
            this.pref = pref;
            this.receiver = new ServiceReceiver(pref);
        }

        public void onPrefClicked() {
            final Context context = pref.getContext();
            final boolean isDefaultBrowser = Browsers.isDefaultBrowser(context);
            final boolean hasDefaultBrowser = Browsers.hasDefaultBrowser(context);

            if (isDefaultBrowser) {
                pref.openAppDetailSettings(context);
            } else if (hasDefaultBrowser) {
                pref.setEnabled(false);
                pref.setSummary(R.string.preference_default_browser_is_setting);
                pref.clearDefaultBrowser(context);
            } else {
                pref.triggerWebOpen();
            }
        }

        public void onFragmentResume() {
            LocalBroadcastManager.getInstance(pref.getContext())
                    .registerReceiver(this.receiver, ComponentToggleService.SERVICE_STOP_INTENT_FILTER);
        }

        public void onFragmentPause() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                LocalBroadcastManager.getInstance(pref.getContext())
                        .unregisterReceiver(this.receiver);
            }
        }
    }

    private static class ServiceReceiver extends BroadcastReceiver {
        DefaultBrowserPreference pref;

        ServiceReceiver(DefaultBrowserPreference pref) {
            this.pref = pref;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // update UI
            pref.update();

            // SettingsActivity is in foreground(because this BroadcastReceiver is working),
            // to remove notification which created by Service
            NotificationManagerCompat.from(context).cancel(ComponentToggleService.NOTIFICATION_ID);

            // if service finished its job, lets fire an intent to choose myself as default browser
            final boolean isDefaultBrowser = Browsers.isDefaultBrowser(context);
            final boolean hasDefaultBrowser = Browsers.hasDefaultBrowser(context);
            if (!isDefaultBrowser && !hasDefaultBrowser) {
                pref.triggerWebOpen();
            }
        }
    }
}
