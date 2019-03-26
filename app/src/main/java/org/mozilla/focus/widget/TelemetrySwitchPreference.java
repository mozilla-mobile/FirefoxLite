/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.FirebaseHelper;
import org.mozilla.focus.utils.SupportUtils;

/**
 * Ideally we'd extend SwitchPreference, and only do the summary modification. Unfortunately
 * that results in us using an older Switch which animates differently to the (seemingly AppCompat)
 * switches used in the remaining preferences. There's no AppCompat SwitchPreference to extend,
 * so instead we just build our own preference.
 */
public class TelemetrySwitchPreference extends Preference {
    public TelemetrySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TelemetrySwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // We are keeping track of the preference value ourselves.
        setPersistent(false);
    }

    @Override
    protected void onBindView(final View view) {
        final Switch switchWidget = (Switch) view.findViewById(R.id.switch_widget);

        switchWidget.setChecked(TelemetryWrapper.isTelemetryEnabled(getContext()));

        switchWidget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TelemetryWrapper.setTelemetryEnabled(getContext(), isChecked);
                // we should use the value from UI (isChecked) instead of relying on SharePreference.
                FirebaseHelper.enableAnalytics(getContext().getApplicationContext(), isEnabled());
            }
        });

        final TextView learnMore = (TextView) view.findViewById(R.id.learnMore);

        learnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is a hardcoded link: if we ever end up needing more of these links, we should
                // move the link into an xml parameter, but there's no advantage to making it configurable now.
                final String url = SupportUtils.getSumoURLForTopic(getContext(), "usage-data");
                final String title = getTitle().toString();

                final Intent intent = InfoActivity.getIntentFor(getContext(), url, title);
                getContext().startActivity(intent);
                TelemetryWrapper.settingsLearnMoreClickEvent(getContext().getString(R.string.pref_key_telemetry));
            }
        });

        // We still want to allow toggling the pref by touching any part of the pref (except for
        // the "learn more" link)
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                switchWidget.toggle();
                return true;
            }
        });

        final String appName = getContext().getResources().getString(R.string.app_name);
        final String mozilla = getContext().getResources().getString(R.string.mozilla);
        setSummary(getContext().getResources().getString(R.string.preference_mozilla_telemetry_summary, appName, mozilla));

        super.onBindView(view);
    }
}
