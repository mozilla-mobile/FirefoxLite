/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.Preference;
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
    private Switch switchView;

    @SuppressWarnings("unused") // Instantiated from XML
    public DefaultBrowserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.preference_default_browser);
    }

    @SuppressWarnings("unused") // Instantiated from XML
    public DefaultBrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_default_browser);
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
            }

            Settings.updatePrefDefaultBrowserIfNeeded(getContext(), isDefaultBrowser);
        }
    }

    @Override
    protected void onClick() {
        final Context context = getContext();
        // fire an intent and start related activity immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openDefaultAppsSettings(context);
        } else {

            final boolean isDefaultBrowser = Browsers.isDefaultBrowser(getContext());
            final boolean hasDefaultBrowser = Browsers.hasDefaultBrowser(getContext());

            if (isDefaultBrowser) {
                openAppDetailSettings(context);
            } else if (hasDefaultBrowser) {
                setEnabled(false);
                setSummary(R.string.preference_default_browser_is_setting);
                clearDefaultBrowser(context);
            } else {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("http://mozilla.org"));

                //  Put a mojo to force MainActivity finish it's self, we probably need an intent flag to handle the task problem (reorder/parent/top)
                viewIntent.putExtra("resolve_default_browser", true);
                context.startActivity(viewIntent);
            }


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

}
