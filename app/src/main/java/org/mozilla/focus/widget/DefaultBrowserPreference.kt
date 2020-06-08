/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.widget

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.extension.toFragmentActivity
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel.DefaultBrowserPreferenceUiModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@TargetApi(Build.VERSION_CODES.N)
class DefaultBrowserPreference : Preference {
    @Inject
    lateinit var viewModelCreator: Lazy<DefaultBrowserPreferenceViewModel>

    private lateinit var viewModel: DefaultBrowserPreferenceViewModel

    private var switchView: Switch? = null

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

    private fun init() {
        appComponent().inject(this)
    }

    override fun onAttachedToActivity() {
        super.onAttachedToActivity()
        viewModel = getActivityViewModel(viewModelCreator)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        switchView = view.findViewById<View>(R.id.switch_widget) as Switch?

        viewModel.uiModel.observe(context.toFragmentActivity(), Observer { update(it) })
        viewModel.openDefaultAppsSettings.observe(context.toFragmentActivity(), Observer { openDefaultAppsSettings() })
        viewModel.openAppDetailSettings.observe(context.toFragmentActivity(), Observer { openAppDetailSettings() })
        viewModel.openSumoPage.observe(context.toFragmentActivity(), Observer { openSumoPage() })
        viewModel.triggerWebOpen.observe(context.toFragmentActivity(), Observer { triggerWebOpen() })
        viewModel.openDefaultAppsSettingsTutorialDialog.observe(context.toFragmentActivity(), Observer { DialogUtils.showGoToSystemAppsSettingsDialog(context, viewModel) })
        viewModel.openUrlTutorialDialog.observe(context.toFragmentActivity(), Observer { DialogUtils.showOpenUrlDialog(context, viewModel) })
        viewModel.successToSetDefaultBrowser.observe(context.toFragmentActivity(), Observer { showSuccessMessage() })
        viewModel.failToSetDefaultBrowser.observe(context.toFragmentActivity(), Observer { showFailMessage() })
    }

    fun update(uiModel: DefaultBrowserPreferenceUiModel) {
        switchView?.let {
            it.isChecked = uiModel.isDefaultBrowser
            Settings.updatePrefDefaultBrowserIfNeeded(context, uiModel.isDefaultBrowser, uiModel.hasDefaultBrowser)
        }
    }

    override fun onClick() {
        viewModel.performAction()
    }

    fun onFragmentResume() {
        viewModel.onResume()
    }

    fun onFragmentPause() {
        viewModel.onPause()
    }

    fun performClick() {
        viewModel.performActionFromNotification()
    }

    private fun openDefaultAppsSettings() {
        if (!IntentUtils.openDefaultAppsSettings(context)) {
            openSumoPage()
        }
    }

    private fun openAppDetailSettings() {
        //  TODO: extract this to util module
        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        //  fromParts might be faster than parse: ex. Uri.parse("package://"+context.getPackageName());
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    private fun openSumoPage() {
        val intent = InfoActivity.getIntentFor(context, SupportUtils.getSumoURLForTopic(context, "rocket-default"), title.toString())
        context.startActivity(intent)
    }

    private fun triggerWebOpen() {
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.data = Uri.parse(SupportUtils.getSumoURLForTopic(context, "rocket-default"))

        //  Put a mojo to force MainActivity finish it's self, we probably need an intent flag to handle the task problem (reorder/parent/top)
        viewIntent.putExtra(EXTRA_RESOLVE_BROWSER, true)
        context.startActivity(viewIntent)
    }

    private fun showSuccessMessage() {
        val successMessageText = context.getString(R.string.message_set_default_success, context.getString(R.string.app_name))
        Toast.makeText(context, successMessageText, Toast.LENGTH_LONG).show()
    }

    private fun showFailMessage() {
        val rootView = (context as Activity).findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup
        val failMessageText = context.getString(R.string.message_set_default_incomplet, context.getString(R.string.app_name))
        Snackbar.make(rootView, failMessageText, TimeUnit.SECONDS.toMillis(8).toInt())
            .setAction(R.string.private_browsing_dialog_add_shortcut_yes) {
                viewModel.performAction()
                TelemetryWrapper.clickSetDefaultTryAgainSnackBar()
            }.show()
    }

    companion object {
        const val EXTRA_RESOLVE_BROWSER = "_intent_to_resolve_browser_"
    }
}
