package org.mozilla.rocket.nightmode;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;


public interface NightModeListener {
    @Nullable
    List<View> getNightModeCover();

    default void adjustBrightness() {
        for (View nightModeCover : getNightModeCover()) {
            if (nightModeCover == null) {
                break;
            }
            Drawable background = nightModeCover.getBackground();
            if (background == null) {
                return;
            }
            background.setAlpha(255 * AdjustBrightnessDialog.Constants.getBRIGHT_PERCENTAGE() / 100);
        }

    }
}
