/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
@file:Suppress("DEPRECATION")

package org.mozilla.focus.settings

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Looper
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.text.TextUtils
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.locale.LocaleManager
import org.mozilla.focus.locale.Locales
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.DialogUtils.createRateAppDialog
import org.mozilla.focus.utils.DialogUtils.createShareAppDialog
import org.mozilla.focus.utils.FirebaseHelper.getFirebase
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.widget.DefaultBrowserPreference
import org.mozilla.rocket.debugging.DebugActivity.Companion.getStartIntent
import org.mozilla.rocket.deeplink.DeepLinkConstants
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog.Intents.getStartIntentFromSetting
import org.mozilla.rocket.privately.ShortcutUtils.Companion.createShortcut
import org.mozilla.telemetry.TelemetryHolder
import java.util.Locale

class SettingsFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
    private var localeUpdated = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)
        val rootPreferences = findPreference(PREF_KEY_ROOT) as PreferenceScreen?
        if (!AppConstants.isDevBuild() && !AppConstants.isFirebaseBuild() && !AppConstants.isNightlyBuild()) {
            val experimentCategory = findPreference(getString(R.string.pref_key_category_experiment))
            rootPreferences?.removePreference(experimentCategory)
            val debuggingCategory = findPreference(getString(R.string.pref_key_category_debug))
            rootPreferences?.removePreference(debuggingCategory)
        }
        val preferenceNightMode = findPreference(getString(R.string.pref_key_night_mode_brightness))
        preferenceNightMode?.isEnabled = Settings.getInstance(activity).isNightModeEnable

        if (DeepLinkConstants.COMMAND_SET_DEFAULT_BROWSER == arguments.getString(SettingsActivity.EXTRA_ACTION)) {
            triggerSetDefaultBrowserAction()
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        val resources = resources
        val keyClicked = preference.key
        TelemetryWrapper.settingsClickEvent(keyClicked)
        when (keyClicked) {
            resources.getString(R.string.pref_key_give_feedback) -> {
                createRateAppDialog(preference.context).show()
            }
            resources.getString(R.string.pref_key_share_with_friends) -> {
                if (!debuggingFirebase()) {
                    createShareAppDialog(preference.context).show()
                }
            }
            resources.getString(R.string.pref_key_about) -> {
                val intent = InfoActivity.getAboutIntent(preference.context)
                startActivity(intent)
            }
            resources.getString(R.string.pref_key_night_mode_brightness) -> {
                Settings.getInstance(preference.context).setNightModeSpotlight(true)
                startActivity(getStartIntentFromSetting(preference.context))
            }
            resources.getString(R.string.pref_key_default_browser) -> {
                TelemetryWrapper.clickDefaultBrowserInSetting()
            }
            resources.getString(R.string.pref_key_private_mode_shortcut) -> {
                TelemetryWrapper.clickPrivateShortcutItemInSettings()
                createShortcut(preference.context.applicationContext)
            }
            resources.getString(R.string.pref_key_debug_page) -> {
                startActivity(getStartIntent(preference.context))
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    private fun debuggingFirebase(): Boolean {
        debugClicks++
        if (debugClicks > DEBUG_CLICKS_THRESHOLD) {
            val debugShare = Intent()
            debugShare.action = Intent.ACTION_SEND
            debugShare.type = "text/plain"
            val testingId = "${getFirebase().getFcmToken()}\n\n${TelemetryHolder.get().clientId}"
            debugShare.putExtra(Intent.EXTRA_TEXT, testingId)
            startActivity(Intent.createChooser(debugShare, "This token is only for QA to test in Nightly and debug build"))
            return true
        }
        return false
    }

    fun onNewIntent(intent: Intent) {
        if (DeepLinkConstants.COMMAND_SET_DEFAULT_BROWSER == intent.getStringExtra(SettingsActivity.EXTRA_ACTION)) {
            triggerSetDefaultBrowserAction()
        }
    }

    private fun triggerSetDefaultBrowserAction() {
        Looper.myQueue().addIdleHandler {
            if (!activity.isFinishing && !activity.isDestroyed) {
                val preference = findPreference(getString(R.string.pref_key_default_browser)) as DefaultBrowserPreference?
                preference?.performClick()
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val preference = findPreference(getString(R.string.pref_key_default_browser)) as DefaultBrowserPreference?
        preference?.onFragmentResume()
    }

    override fun onPause() {
        super.onPause()
        val preference = findPreference(getString(R.string.pref_key_default_browser)) as DefaultBrowserPreference?
        preference?.onFragmentPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // special handling for locale selection
        if (key == getString(R.string.pref_key_locale)) {
            // Updating the locale leads to onSharedPreferenceChanged being triggered again in some
            // cases. To avoid an infinite loop we won't update the preference a second time. This
            // fragment gets replaced at the end of this method anyways.
            if (localeUpdated) {
                return
            }
            localeUpdated = true
            val languagePreference = findPreference(getString(R.string.pref_key_locale)) as ListPreference?
            languagePreference?.let {
                val value = it.value
                val localeManager = LocaleManager.getInstance()
                val locale: Locale
                if (TextUtils.isEmpty(value)) {
                    localeManager.resetToSystemLocale(activity)
                    locale = localeManager.getCurrentLocale(activity)
                } else {
                    locale = Locales.parseLocaleCode(value)
                    localeManager.setSelectedLocale(activity, value)
                }
                TelemetryWrapper.settingsLocaleChangeEvent(key, locale.toString(), TextUtils.isEmpty(value))
                localeManager.updateConfiguration(activity, locale)

                activity?.apply {
                    // Manually notify SettingsActivity of locale changes (in most other cases activities
                    // will detect changes in onActivityResult(), but that doesn't apply to SettingsActivity).
                    onConfigurationChanged(resources.configuration)

                    // And ensure that the calling LocaleAware*Activity knows that the locale changed:
                    setResult(SettingsActivity.ACTIVITY_RESULT_LOCALE_CHANGED)
                }
                // The easiest way to ensure we update the language is by replacing the entire fragment:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, SettingsFragment())
                    .commit()
            }
            return
        } else if (key != getString(R.string.pref_key_telemetry)) {
            // We'll handle the pref_key_telemetry by TelemetrySwitchPreference.
            // For other events, we handle them here.
            TelemetryWrapper.settingsEvent(key, sharedPreferences.all[key].toString(), false)
        }
    }

    companion object {
        private var debugClicks = 0
        private const val DEBUG_CLICKS_THRESHOLD = 19
        private const val PREF_KEY_ROOT = "root_preferences"
        const val TAG = "SettingsFragment"

        @JvmStatic
        fun newInstance(action: String): SettingsFragment {
            val args = Bundle().apply {
                putString(SettingsActivity.EXTRA_ACTION, action)
            }
            return SettingsFragment().apply {
                arguments = args
            }
        }
    }
}
