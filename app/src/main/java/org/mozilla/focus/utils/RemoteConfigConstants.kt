package org.mozilla.focus.utils


object RemoteConfigConstants {
    val FEATURE_SURVEY_DEFAULT = SURVEY.NONE.value

    enum class SURVEY constructor(val value: Int) {
        NONE(0), WIFI_FINDING(1), VPN(2);

        companion object {

            fun parseLong(index: Long): SURVEY {
                return when (index) {
                    WIFI_FINDING.value.toLong() -> WIFI_FINDING
                    VPN.value.toLong() -> VPN
                    else -> NONE
                }
            }
        }
    }
}
