package org.mozilla.focus.widget

import android.os.Bundle
import android.view.View
import android.widget.SeekBar

import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.utils.Settings

class AdjustBrightnessDialog : BaseActivity() {

    object Constants {
        // Only allow user to adjust brightness from 0.0f to 0.5f of system brightness, default is 0.25f
        const val DEFAULT_BRIGHTNESS = 0.25f
        const val MULTIPLIER = 2 * 100
    }

    private lateinit var seekBar: SeekBar

    private val mSeekListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                val layoutParams = window.attributes
                val value = progress.toFloat() / Constants.MULTIPLIER
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
    }

    override fun onResume() {
        super.onResume()
        val currentBrightness = Settings.getInstance(this).nightModeBrightnessValue
        val progress = (currentBrightness * Constants.MULTIPLIER).toInt()
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
