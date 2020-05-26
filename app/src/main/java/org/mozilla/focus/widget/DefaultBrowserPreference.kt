/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.widget

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.Switch
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.components.ComponentToggleService
import org.mozilla.focus.telemetry.TelemetryWrapper.onDefaultBrowserServiceFailed
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.SupportUtils

@TargetApi(Build.VERSION_CODES.N)
class DefaultBrowserPreference : Preference {
    private var switchView: Switch? = null
    private lateinit var action: DefaultBrowserAction

    // Instantiated from XML
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        widgetLayoutResource = R.layout.preference_default_browser
        init()
    }

    // Instantiated from XML
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        widgetLayoutResource = R.layout.preference_default_browser
        init()
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        switchView = view.findViewById<View>(R.id.switch_widget) as Switch?
        update()
    }

    fun update() {
        switchView?.let {
            val isDefaultBrowser = Browsers.isDefaultBrowser(context)
            val hasDefaultBrowser = Browsers.hasDefaultBrowser(context)
            it.isChecked = isDefaultBrowser
            if (ComponentToggleService.isAlive(context)) {
                isEnabled = false
                setSummary(R.string.preference_default_browser_is_setting)
            } else {
                isEnabled = true
                summary = null
            }
            Settings.updatePrefDefaultBrowserIfNeeded(context, isDefaultBrowser, hasDefaultBrowser)
        }
    }

    override fun onClick() {
        action.onPrefClicked()
    }

    fun onFragmentResume() {
        update()
        action.onFragmentResume()
    }

    fun onFragmentPause() {
        action.onFragmentPause()
    }

    private fun init() {
        action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            DefaultAction(this)
        } else {
            LowSdkAction(this)
        }
    }

    private fun openAppDetailSettings(context: Context) {
        //  TODO: extract this to util module
        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        //  fromParts might be faster than parse: ex. Uri.parse("package://"+context.getPackageName());
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    private fun clearDefaultBrowser(context: Context) {
        val intent = Intent()
        intent.component = ComponentName(context, ComponentToggleService::class.java)
        context.startService(intent)
    }

    private fun openSumoPage(context: Context) {
        val intent = InfoActivity.getIntentFor(context, SupportUtils.getSumoURLForTopic(context, "rocket-default"), title.toString())
        context.startActivity(intent)
    }

    private fun triggerWebOpen() {
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.data = Uri.parse("http://mozilla.org")

        //  Put a mojo to force MainActivity finish it's self, we probably need an intent flag to handle the task problem (reorder/parent/top)
        viewIntent.putExtra(EXTRA_RESOLVE_BROWSER, true)
        context.startActivity(viewIntent)
    }

    /**
     * To define necessary actions for setting default-browser.
     */
    private interface DefaultBrowserAction {
        fun onPrefClicked()
        fun onFragmentResume()
        fun onFragmentPause()
    }

    private class DefaultAction internal constructor(var pref: DefaultBrowserPreference) : DefaultBrowserAction {
        override fun onPrefClicked() {
            // fire an intent and start related activity immediately
            if (!IntentUtils.openDefaultAppsSettings(pref.context)) {
                pref.openSumoPage(pref.context)
            }
        }

        override fun onFragmentResume() {}
        override fun onFragmentPause() {}
    }

    /**
     * For android sdk version older than N
     */
    private class LowSdkAction internal constructor(var pref: DefaultBrowserPreference) : DefaultBrowserAction {
        var receiver: BroadcastReceiver = ServiceReceiver(pref)
        override fun onPrefClicked() {
            val context = pref.context
            val isDefaultBrowser = Browsers.isDefaultBrowser(context)
            val hasDefaultBrowser = Browsers.hasDefaultBrowser(context)
            if (isDefaultBrowser) {
                pref.openAppDetailSettings(context)
            } else if (hasDefaultBrowser) {
                pref.isEnabled = false
                pref.setSummary(R.string.preference_default_browser_is_setting)
                pref.clearDefaultBrowser(context)
            } else {
                pref.triggerWebOpen()
            }
        }

        override fun onFragmentResume() {
            LocalBroadcastManager.getInstance(pref.context)
                .registerReceiver(receiver, ComponentToggleService.SERVICE_STOP_INTENT_FILTER)
        }

        override fun onFragmentPause() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                LocalBroadcastManager.getInstance(pref.context)
                    .unregisterReceiver(receiver)
            }
        }
    }

    private class ServiceReceiver internal constructor(var pref: DefaultBrowserPreference) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // update UI
            pref.update()

            // SettingsActivity is in foreground(because this BroadcastReceiver is working),
            // to remove notification which created by Service
            NotificationManagerCompat.from(context).cancel(ComponentToggleService.NOTIFICATION_ID)
            val isDefaultBrowser = Browsers.isDefaultBrowser(context)
            val hasDefaultBrowser = Browsers.hasDefaultBrowser(context)

            // The default-browser-config should be cleared, if the service finished its job.
            // if not been cleared, we regards it as 'fail'
            if (hasDefaultBrowser && !isDefaultBrowser) {
                onDefaultBrowserServiceFailed()
            }

            // if service finished its job, lets fire an intent to choose myself as default browser
            if (!isDefaultBrowser && !hasDefaultBrowser) {
                pref.triggerWebOpen()
            }
        }
    }

    companion object {
        const val EXTRA_RESOLVE_BROWSER = "_intent_to_resolve_browser_"
    }
}