/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.rocket.nightmode.themed.ThemedImageView;
import org.mozilla.rocket.theme.ThemeManager;

public class HomeScreenBackground extends ThemedImageView implements ThemeManager.Themeable {
    private Paint paint;
    private boolean isNight;

    public HomeScreenBackground(Context context) {
        super(context, null);
        init();
    }

    public HomeScreenBackground(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        init();
    }

    public HomeScreenBackground(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        final Rect rect = new Rect();
        ((Activity) getContext()).getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.home_pattern);
        paint = new Paint();
        Shader shader1 = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        int colors[] = {Color.parseColor("#99FFFFFF"), Color.parseColor("#4dFFFFFF"), Color.parseColor("#1aFFFFFF"), Color.parseColor("#00FFFFFF")};
        float positions[] = {0.0f, 0.4f, 0.7f, 1f};
        Shader shader2 = new LinearGradient(0, rect.top, 0, rect.bottom, colors, positions, Shader.TileMode.CLAMP);
        paint.setShader(new ComposeShader(shader2, shader1, PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.isNight) {
            // Add status bar's height as a padding on top to let HomeFragment star background align with TabTrayFragment
            setPadding(0, ViewUtils.getStatusBarHeight(((Activity) getContext())), 0, 0);
        } else {
            setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onThemeChanged() {
        Drawable drawable = getContext().getTheme().getDrawable(R.drawable.bg_homescreen_color);
        setBackground(drawable);
    }

    @Override
    public void setNightMode(boolean isNight) {
        super.setNightMode(isNight);
        this.isNight = isNight;
        if (this.isNight) {
            setImageResource(R.drawable.star_bg);
        } else {
            setImageResource(R.drawable.firefox_lite_bg);
        }
    }
}
