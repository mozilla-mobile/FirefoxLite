/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.widget

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.Switch
import androidx.lifecycle.Observer
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.extension.toFragmentActivity
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel.DefaultBrowserPreferenceUiModel
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
        viewIntent.data = Uri.parse("http://mozilla.org")

        //  Put a mojo to force MainActivity finish it's self, we probably need an intent flag to handle the task problem (reorder/parent/top)
        viewIntent.putExtra(EXTRA_RESOLVE_BROWSER, true)
        context.startActivity(viewIntent)
    }

    companion object {
        const val EXTRA_RESOLVE_BROWSER = "_intent_to_resolve_browser_"
    }
}
