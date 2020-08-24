/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.mozilla.focus.R;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.FirebaseHelper;

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
        setWidgetLayoutResource(R.layout.preference_telemetry);
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

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                switchWidget.toggle();
                return true;
            }
        });

        super.onBindView(view);
    }
}
