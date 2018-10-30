package org.mozilla.rocket.nightmode

import android.content.Intent

class Intent {
    companion object {
        private const val EXTRA_NIGHT_MODE_SOURCE_FROM_SETTING = "source_from_setting"
        private const val ACTION_ADJUST_BRIGHTNESS = "android.intent.action.ADJUST_BRIGHTNESS"

        fun getNightModeBrightnessIntent(fromSetting: Boolean): Intent {
            val intent = Intent(ACTION_ADJUST_BRIGHTNESS, null)
            intent.putExtra(EXTRA_NIGHT_MODE_SOURCE_FROM_SETTING, fromSetting)
            return intent
        }

        fun isNightModeBrightnessSourceFromSetting(intent: Intent): Boolean {
            return intent.getBooleanExtra(EXTRA_NIGHT_MODE_SOURCE_FROM_SETTING, true)
        }
    }
}