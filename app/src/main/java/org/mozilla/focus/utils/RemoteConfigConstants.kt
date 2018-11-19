package org.mozilla.focus.utils

object RemoteConfigConstants {
    val FEATURE_SURVEY_DEFAULT = SURVEY.NONE.value

    enum class SURVEY constructor(val value: Int) {
        NONE(0), WIFI_FINDING(1), VPN(2), VPN_RECOMMENDER(3);

        companion object {

            fun parseLong(index: Long): SURVEY {
                return when (index) {
                    WIFI_FINDING.value.toLong() -> WIFI_FINDING
                    VPN.value.toLong() -> VPN
                    VPN_RECOMMENDER.value.toLong() -> VPN_RECOMMENDER
                    else -> NONE
                }
            }
        }
    }
}
