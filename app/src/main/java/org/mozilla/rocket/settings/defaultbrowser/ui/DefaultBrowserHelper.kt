package org.mozilla.rocket.settings.defaultbrowser.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.widget.DefaultBrowserPreference
import java.util.concurrent.TimeUnit

class DefaultBrowserHelper(
    private val activity: Activity,
    private val defaultBrowserPreferenceViewModel: DefaultBrowserPreferenceViewModel
) {

    fun openDefaultAppsSettings() {
        if (!IntentUtils.openDefaultAppsSettings(activity)) {
            openSumoPage()
        }
    }

    fun openAppDetailSettings() {
        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        //  fromParts might be faster than parse: ex. Uri.parse("package://"+context.getPackageName());
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    fun openSumoPage() {
        val intent = InfoActivity.getIntentFor(
            activity,
            SupportUtils.getSumoURLForTopic(activity, "rocket-default"),
            activity.getString(R.string.preference_default_browser)
        )
        activity.startActivity(intent)
    }

    fun triggerWebOpen() {
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.data = Uri.parse(SupportUtils.getSumoURLForTopic(activity, "rocket-default"))

        //  Put a mojo to force MainActivity finish it's self, we probably need an intent flag to handle the task problem (reorder/parent/top)
        viewIntent.putExtra(DefaultBrowserPreference.EXTRA_RESOLVE_BROWSER, true)
        activity.startActivity(viewIntent)
    }

    fun showSuccessMessage() {
        val successMessageText = activity.getString(R.string.message_set_default_success, activity.getString(R.string.app_name))
        Toast.makeText(activity, successMessageText, Toast.LENGTH_LONG).show()
    }

    fun showFailMessage() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup
        val failMessageText = activity.getString(R.string.message_set_default_incomplet, activity.getString(R.string.app_name))
        Snackbar.make(rootView, failMessageText, TimeUnit.SECONDS.toMillis(8).toInt())
            .setAction(R.string.private_browsing_dialog_add_shortcut_yes) {
                defaultBrowserPreferenceViewModel.performAction()
                TelemetryWrapper.clickSetDefaultTryAgainSnackBar()
            }.show()
    }
}