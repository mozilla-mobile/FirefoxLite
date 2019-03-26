/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.locale.LocaleManager;
import org.mozilla.focus.locale.Locales;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.*;
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog;
import org.mozilla.focus.widget.DefaultBrowserPreference;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean localeUpdated;
    private static int debugClicks = 0;
    private static final int DEBUG_CLICKS_THRESHOLD = 19;
    private final static String PREF_KEY_ROOT = "root_preferences";
    private boolean hasContentPortal = false;

    @Override
    public void onAttach (Context context) {
        super.onAttach(context);
        hasContentPortal = AppConfigWrapper.hasNewsPortal();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        final PreferenceScreen rootPreferences = (PreferenceScreen) findPreference(PREF_KEY_ROOT);
        if (!AppConstants.isDevBuild() && !AppConstants.isFirebaseBuild() && !AppConstants.isNightlyBuild()) {
            Preference category = findPreference(getString(R.string.pref_key_category_development));
            rootPreferences.removePreference(category);
        }
        if (!hasContentPortal) {
            Preference category = findPreference(getString(R.string.pref_s_news));
            rootPreferences.removePreference(category);
        }

        final Preference preferenceNightMode = findPreference(getString(R.string.pref_key_night_mode_brightness));
        preferenceNightMode.setEnabled(Settings.getInstance(getActivity()).isNightModeEnable());

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Resources resources = getResources();
        String keyClicked = preference.getKey();

        TelemetryWrapper.settingsClickEvent(keyClicked);

        if (keyClicked.equals(resources.getString(R.string.pref_key_give_feedback))) {
            DialogUtils.showRateAppDialog(getActivity());
        } else if (keyClicked.equals(resources.getString(R.string.pref_key_share_with_friends))) {
            if (!debugingFirebase()) {
                DialogUtils.showShareAppDialog(getActivity());
            }
        } else if (keyClicked.equals(resources.getString(R.string.pref_key_about))) {
            final Intent intent = InfoActivity.getAboutIntent(getActivity());
            startActivity(intent);
        } else if (keyClicked.equals(resources.getString(R.string.pref_key_night_mode_brightness))) {
            Settings.getInstance(getActivity()).setNightModeSpotlight(true);
            startActivity(AdjustBrightnessDialog.Intents.INSTANCE.getStartIntentFromSetting(getActivity()));
        } else if (keyClicked.equals(resources.getString(R.string.pref_key_default_browser))) {
            TelemetryWrapper.clickDefaultBrowserInSetting();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean debugingFirebase() {
        debugClicks++;
        if (debugClicks > DEBUG_CLICKS_THRESHOLD) {
            final Intent debugShare = new Intent();
            debugShare.setAction(Intent.ACTION_SEND);
            debugShare.setType("text/plain");
            final FirebaseContract firebase = FirebaseHelper.firebaseContract;
            if (firebase != null) {
                debugShare.putExtra(Intent.EXTRA_TEXT, firebase.getFcmToken());
            }
            startActivity(Intent.createChooser(debugShare, "This token is only for QA to test in Nightly and debug build"));
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        final DefaultBrowserPreference preference = (DefaultBrowserPreference) findPreference(getString(R.string.pref_key_default_browser));
        if (preference != null) {
            preference.onFragmentResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final DefaultBrowserPreference preference = (DefaultBrowserPreference) findPreference(getString(R.string.pref_key_default_browser));
        if (preference != null) {
            preference.onFragmentPause();
        }
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // special handling for locale selection
        if (key.equals(getString(R.string.pref_key_locale))) {
            // Updating the locale leads to onSharedPreferenceChanged being triggered again in some
            // cases. To avoid an infinite loop we won't update the preference a second time. This
            // fragment gets replaced at the end of this method anyways.
            if (localeUpdated) {
                return;
            }
            localeUpdated = true;

            final ListPreference languagePreference = (ListPreference) findPreference(getString(R.string.pref_key_locale));
            final String value = languagePreference.getValue();

            final LocaleManager localeManager = LocaleManager.getInstance();

            final Locale locale;
            if (TextUtils.isEmpty(value)) {
                localeManager.resetToSystemLocale(getActivity());
                locale = localeManager.getCurrentLocale(getActivity());
            } else {
                locale = Locales.parseLocaleCode(value);
                localeManager.setSelectedLocale(getActivity(), value);
            }
            TelemetryWrapper.settingsLocaleChangeEvent(key, String.valueOf(locale), TextUtils.isEmpty(value));
            localeManager.updateConfiguration(getActivity(), locale);

            // Manually notify SettingsActivity of locale changes (in most other cases activities
            // will detect changes in onActivityResult(), but that doesn't apply to SettingsActivity).
            getActivity().onConfigurationChanged(getActivity().getResources().getConfiguration());

            // And ensure that the calling LocaleAware*Activity knows that the locale changed:
            getActivity().setResult(SettingsActivity.ACTIVITY_RESULT_LOCALE_CHANGED);

            // The easiest way to ensure we update the language is by replacing the entire fragment:
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .commit();
            return;
            // we'll handle the pref_key_telemetry by TelemetrySwitchPreference
        } else if (!key.equals(getString(R.string.pref_key_telemetry))) {
            // For other events, we handle them here.
            TelemetryWrapper.settingsEvent(key, String.valueOf(sharedPreferences.getAll().get(key)), false);
        }


        if (key.equals(getString(R.string.pref_key_storage_clear_browsing_data))) {
            //Clear browsing data Callback function is not here
            //Go to Class CleanBrowsingDataPreference -> onDialogClosed
        } else if (key.equals(getString(R.string.pref_key_storage_save_downloads_to))) {
            //Save downloads/cache/offline pages to SD card/Internal storage Callback function
        }
    }
}
