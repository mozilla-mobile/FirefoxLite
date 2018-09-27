package org.mozilla.focus.widget;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;

import org.mozilla.focus.R;

public class BrightnessDialog extends Activity {

    private SeekBar seekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = getWindow();
        window.setGravity(Gravity.TOP);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.adjust_briteness_view);
//        final ImageView icon = (ImageView) findViewById(R.id.brightness_icon);
//        final ToggleSlider slider = (ToggleSlider) findViewById(R.id.brightness_slider);
//        mBrightnessController = new BrightnessController(this, icon, slider);
        seekBar = findViewById(R.id.brightness_slider);
        seekBar.setOnSeekBarChangeListener(mSeekListener);
        findViewById(R.id.brightness_root).setOnClickListener(v->{
            finish();
        });
        Log.e("ramoss", "joseph on create");
    }
    @Override
    protected void onStart() {
        super.onStart();
//        mBrightnessController.registerCallbacks();
//        MetricsLogger.visible(this, MetricsEvent.BRIGHTNESS_DIALOG);
    }
    @Override
    protected void onStop() {
        super.onStop();
//        MetricsLogger.hidden(this, MetricsEvent.BRIGHTNESS_DIALOG);
//        mBrightnessController.unregisterCallbacks();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Log.e("ramoss", "onProgressChanged = " + progress);
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes(); // Get Params
            float BackLightValue = (float)progress/100;
            layoutParams.screenBrightness = BackLightValue; // Set Value
            getWindow().setAttributes(layoutParams); // Set params
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
}
