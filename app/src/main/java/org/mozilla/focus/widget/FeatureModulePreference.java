/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.FeatureModule;

@TargetApi(Build.VERSION_CODES.N)
public class FeatureModulePreference extends Preference {
    private Switch switchView;

    @SuppressWarnings("unused") // Instantiated from XML
    public FeatureModulePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.preference_feature_module);
    }

    @SuppressWarnings("unused") // Instantiated from XML
    public FeatureModulePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_feature_module);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        switchView = (Switch) view.findViewById(R.id.switch_widget);
        update();
    }

    public void update() {
        if (switchView != null) {
            final boolean installed = FeatureModule.getInstance().isSupportPrivateBrowsing();
            switchView.setChecked(installed);
        }

        Log.d("Rocket", "clear summary");
        setSummary("May the Gecko be with you");
    }

    @Override
    protected void onClick() {
        final FeatureModule featureModule = FeatureModule.getInstance();
        setEnabled(false);
        if (featureModule.isSupportPrivateBrowsing()) {
            featureModule.uninstall(getContext(), new FeatureModule.StatusListener() {
                @Override
                public void onDone() {
                    setEnabled(true);
                    update();
                }

                @Override
                public void onProgress(String msg) {
                }
            });
        } else {
            featureModule.install(getContext(), new FeatureModule.StatusListener() {
                @Override
                public void onDone() {
                    setEnabled(true);
                    update();
                }

                @Override
                public void onProgress(String msg) {
                    Log.d("Rocket", msg);
                    setSummary(msg);
                }
            });
        }
    }
}
