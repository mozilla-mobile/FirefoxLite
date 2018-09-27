package org.mozilla.focus.home

import android.content.Context
import android.os.Handler
import androidx.cardview.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.RemoteConfigConstants
import org.mozilla.focus.utils.Settings

class FeatureSurveyViewHelper internal constructor(private val context: Context, private val featureSurvey: RemoteConfigConstants.SURVEY) : View.OnClickListener {

    object Constants {
        const val DISMISS_DELAY: Long = 5000
    }

    private var isViewInit = false
    private lateinit var parentView: ViewGroup
    private lateinit var rootView: View
    private lateinit var textContent: TextView
    private lateinit var btnYes: Button
    private lateinit var btnNo: Button

    override fun onClick(v: View) {
        parentView = v.parent as ViewGroup
        if (!isViewInit) {
            isViewInit = true
            LayoutInflater.from(context).inflate(R.layout.wifi_vpn_survey, parentView)
            rootView = parentView.findViewById(R.id.wifi_vpn_root)
            textContent = rootView.findViewById(R.id.wifi_vpn_text_content)
            btnYes = rootView.findViewById(R.id.wifi_vpn_btn_yes)
            btnNo = rootView.findViewById(R.id.wifi_vpn_btn_no)
            val cardView: CardView = rootView.findViewById(R.id.wifi_vpn_card)
            // do nothing when click the whole message
            cardView.setOnClickListener(null)

            if (featureSurvey == RemoteConfigConstants.SURVEY.WIFI_FINDING) {
                textContent.setText(R.string.exp_survey_wifi)
                TelemetryWrapper.clickWifiFinderSurvey()
            } else if (featureSurvey == RemoteConfigConstants.SURVEY.VPN) {
                textContent.setText(R.string.exp_survey_vpn)
                TelemetryWrapper.clickVpnSurvey()
            }

            val eventHistory = Settings.getInstance(context).eventHistory
            // Click outside will dismiss the survey view
            rootView.setOnClickListener { _ ->
                parentView.removeView(rootView)
                isViewInit = false
                if (featureSurvey == RemoteConfigConstants.SURVEY.WIFI_FINDING) {
                    if (eventHistory.contains(Settings.Event.FeatureSurveyWifiFinding)) {
                        v.visibility = View.GONE
                    } else {
                        TelemetryWrapper.dismissWifiFinderSurvey()
                    }
                } else if (featureSurvey == RemoteConfigConstants.SURVEY.VPN) {
                    if (eventHistory.contains(Settings.Event.FeatureSurveyVpn)) {
                        v.visibility = View.GONE
                    } else {
                        TelemetryWrapper.dismissVpnSurvey()
                    }
                }
            }
            btnYes.setOnClickListener { _ ->
                textContent.text = context.getString(R.string.exp_survey_thanks, "\uD83D\uDE00")
                btnYes.visibility = View.GONE
                btnNo.visibility = View.GONE
                if (featureSurvey == RemoteConfigConstants.SURVEY.WIFI_FINDING) {
                    eventHistory.add(Settings.Event.FeatureSurveyWifiFinding)
                    TelemetryWrapper.clickWifiFinderSurveyFeedback(true)
                } else if (featureSurvey == RemoteConfigConstants.SURVEY.VPN) {
                    eventHistory.add(Settings.Event.FeatureSurveyVpn)
                    TelemetryWrapper.clickVpnSurveyFeedback(true)
                }
                dismissSurveyView(v)
            }
            btnNo.setOnClickListener { _ ->
                textContent.text = context.getString(R.string.exp_survey_thanks, "\uD83D\uDE00")
                btnYes.visibility = View.GONE
                btnNo.visibility = View.GONE
                if (featureSurvey == RemoteConfigConstants.SURVEY.WIFI_FINDING) {
                    eventHistory.add(Settings.Event.FeatureSurveyWifiFinding)
                    TelemetryWrapper.clickWifiFinderSurveyFeedback(false)
                } else if (featureSurvey == RemoteConfigConstants.SURVEY.VPN) {
                    eventHistory.add(Settings.Event.FeatureSurveyVpn)
                    TelemetryWrapper.clickVpnSurveyFeedback(false)
                }
                dismissSurveyView(v)
            }
        }
    }

    private fun dismissSurveyView(btn : View) {
        Handler().postDelayed({
            parentView.removeView(rootView)
            isViewInit = false
            btn.visibility = View.GONE
        }, Constants.DISMISS_DELAY)
    }
}
