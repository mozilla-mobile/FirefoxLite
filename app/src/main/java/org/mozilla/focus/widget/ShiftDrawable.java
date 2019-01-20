/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class ShiftDrawable extends DrawableWrapper implements Runnable, Animatable {

    //    private final ValueAnimator mAnimator = ValueAnimator.ofFloat(0f, 1f);
    private Interpolator interpolator = null;

    private Integer duration = null;

    private float currentFraction = 0;

    private float currentRatio = 0;

    volatile private boolean isRunning = false;

    private final Rect mVisibleRect = new Rect();

    private Path mPath;

    // align to ScaleDrawable implementation
    private static final int MAX_LEVEL = 10000;

    private static final int DEFAULT_DURATION = 1000;

    private long animationStart;

    public ShiftDrawable(@NonNull Drawable d) {
        this(d, DEFAULT_DURATION);
    }

    public ShiftDrawable(@NonNull Drawable d, int duration) {
        this(d, duration, new LinearInterpolator());
    }

    public ShiftDrawable(@NonNull Drawable d, int duration, @Nullable Interpolator interpolator) {
        super(d);
    }

    public int getDuration() {
        return (duration == null) ? DEFAULT_DURATION : duration;

    }

    public Interpolator getInterpolator() {
        return (interpolator == null) ? new LinearInterpolator() : interpolator;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean result = super.setVisible(visible, restart);
        // don't need to schedule next frame. Let the caller decides when to start the animation
        // If we are hiding, just
        if (!visible) {
            stop();
        }
        return result;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateBounds();
    }

    @Override
    protected boolean onLevelChange(int level) {
        final boolean result = super.onLevelChange(level);
        updateBounds();
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        final Drawable d = getWrappedDrawable();
        // FIXME: playBackTime is not accurate cause it didn't consider: the real playback time is the time the first frame
        // drawn in vsync's callback, not the time we call start().
        // Consider:
        // a. Save the start time in  AnimationState during Vsync' callback
        // b. use Animator api.
        final long playBackTime = System.currentTimeMillis() - animationStart;
        float fraction = playBackTime / (float) 300;
        fraction = getInterpolator().getInterpolation(fraction);
        currentRatio = evaluate(fraction, currentRatio, 1);
        final int width = mVisibleRect.width();
        final int offset = (int) (width * currentRatio);
        final int stack = canvas.save();

        if (mPath != null) {
            canvas.clipPath(mPath);
        }

        // shift from right to left.
        // draw left-half part
        canvas.save();
        canvas.drawColor(Color.BLUE);

        canvas.translate(-offset, 0);
        d.draw(canvas);
        canvas.restore();

        // draw right-half part
        canvas.save();
        canvas.drawColor(Color.RED);    // I make this RED for debugging
        canvas.translate(width - offset, 0);
        d.draw(canvas);
        canvas.restore();

        canvas.restoreToCount(stack);
    }

    private void updateBounds() {
        final Rect b = getBounds();
        final int width = (int) ((float) b.width() * getLevel() / MAX_LEVEL);
        final float radius = b.height() / 2f;
        mVisibleRect.set(b.left, b.top, b.left + width, b.height());

        // draw round to head of progressbar. I know it looks stupid, don't blame me now.
        mPath = new Path();
        mPath.addRect(b.left, b.top, b.left + width - radius, b.height(), Path.Direction.CCW);
        mPath.addCircle(b.left + width - radius, radius, radius, Path.Direction.CCW);
    }

    @Override
    public void start() {
        if (isRunning)
            return;
        isRunning = true;
        animationStart = System.currentTimeMillis();
        nextFrame();
    }

    @Override
    public void stop() {
        if (!isRunning())
            return;
        isRunning = false;
        unscheduleSelf(this);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {
        if (isRunning) {
            nextFrame();
        } else {
            invalidateSelf();
            unscheduleSelf(this);
        }

    }

    private void nextFrame() {
        invalidateSelf();
        scheduleSelf(this, getDuration());
    }

    // we don't have animator, so evaluate myself
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }
}
