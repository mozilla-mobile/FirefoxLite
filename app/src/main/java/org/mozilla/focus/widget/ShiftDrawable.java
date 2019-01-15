/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.Interpolator;

public class ShiftDrawable extends DrawableWrapper implements Runnable, Animatable {

    private final Rect mVisibleRect = new Rect();

    private Path mPath;

    // align to ScaleDrawable implementation
    private static final int MAX_LEVEL = 10000;

    private boolean isRunning = false;

    private double fraction = 0.0;

    public ShiftDrawable(@NonNull Drawable d, int duration, @Nullable Interpolator interpolator) {
        super(d);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean isDifferent = super.setVisible(visible, restart);
        if (isDifferent && isVisible()) {
            setFrame();
        } else {
            //mAnimator.end();
            unscheduleSelf(this);
        }
        return isDifferent;
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
        final int width = mVisibleRect.width();
        final int offset = (int) (width * fraction);
        final int stack = canvas.save();

        if (mPath != null) {
            canvas.clipPath(mPath);
        }

        // shift from right to left.
        // draw left-half part
        canvas.save();
        canvas.translate(-offset, 0);
        d.draw(canvas);
        canvas.restore();

        // draw right-half part
        canvas.save();
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
        if (isRunning) {
            return;
        }
        isRunning = true;
        setFrame();
    }

    @Override
    public void stop() {
        isRunning = false;
        unscheduleSelf(this);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {
        fraction += 0.1;
    }

    private void setFrame() {
        scheduleSelf(this, 100);
    }
}
