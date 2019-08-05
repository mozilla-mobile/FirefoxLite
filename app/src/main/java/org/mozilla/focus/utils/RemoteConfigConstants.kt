package org.mozilla.focus.utils

object RemoteConfigConstants {

    // Provide different default value for ui testing and production mode
    val FEATURE_SURVEY_DEFAULT = if (!AppConstants.isUnderEspressoTest()) {
        RemoteConfigConstants.SURVEY.NONE
    } else {
        RemoteConfigConstants.SURVEY.VPN_RECOMMENDER
    }.value

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
