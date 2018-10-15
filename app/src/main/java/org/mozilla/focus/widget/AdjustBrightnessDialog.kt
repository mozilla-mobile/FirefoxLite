package org.mozilla.focus.widget

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.ViewUtils

class AdjustBrightnessDialog : BaseActivity() {

    object Constants {
        // Only allow user to adjust brightness from 0.0f to 0.5f of system brightness
        private const val SCALE = 0.5
        //  Value to Progress multiplier
        private const val MULTIPLIER = 100.0f
        // Value default is a quarter of SCALE
        const val DEFAULT_BRIGHTNESS = (0.25 * SCALE).toFloat()

        fun progressToValue(progress: Int) :Float {
            return (progress * SCALE / MULTIPLIER).toFloat()
        }

        fun valueToProgress(value: Float) :Int {
            return (value * MULTIPLIER / SCALE).toInt()
        }
    }

    private lateinit var seekBar: SeekBar

    private val mSeekListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                val layoutParams = window.attributes
                var value = Constants.progressToValue(progress)
                if (value < 0.01) {
                    value = 0.01f
                }
                layoutParams.screenBrightness = value
                window.attributes = layoutParams
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.adjust_briteness_view)
        seekBar = findViewById(R.id.brightness_slider)
        seekBar.setOnSeekBarChangeListener(mSeekListener)
        findViewById<View>(R.id.brightness_root).setOnClickListener { finish() }
        ViewUtils.updateStatusBarStyle(false, window)
    }

    override fun onResume() {
        super.onResume()
        val currentBrightness = Settings.getInstance(this).nightModeBrightnessValue
        val progress = Constants.valueToProgress(currentBrightness)
        seekBar.progress = progress
    }

    override fun onPause() {
        super.onPause()
        val layoutParams = window.attributes
        Settings.getInstance(this).nightModeBrightnessValue = layoutParams.screenBrightness
    }

    override fun applyLocale() {

    }
}
