/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.widget;

import android.view.animation.Interpolator;

import static java.lang.Math.*;

/*
 * Provide more control over BounceInterpolator
 *
 * Source: https://gist.github.com/Kraiden/1390f1ac52a04e08b83e2d8b5a408aca
 *
 * */
public class BetterBounceInterpolator implements Interpolator {
    private int mBounces;
    private double mEnergy;

    /**
     * Have more control over how to bounce your values.
     */
    public BetterBounceInterpolator() {
        this(3);
    }

    /**
     * Have more control over how to bounce your values.
     *
     * @param bounces number of times to bounce before coming to a rest
     */
    public BetterBounceInterpolator(int bounces) {
        this(bounces, 0.3f);
    }

    /**
     * Have more control over how to bounce your values.
     *
     * @param bounces      number of times to bounce before coming to a rest
     * @param energyFactor control how the bounce loses momentum. 0 is lose energy linearly. 1 energy loss slows down over time, -1 energy loss speeds up over time. Values greater than 1 and less than -1 cause over/under bounce
     */
    public BetterBounceInterpolator(int bounces, double energyFactor) {
        mBounces = bounces;
        mEnergy = energyFactor + 0.5;
    }

    @Override
    public float getInterpolation(float x) {
        return (float) (1d + (-abs(cos(x * 10 * mBounces / PI)) * getCurveAdjustment(x)));
    }

    private double getCurveAdjustment(double x) {
        return -(2 * (1 - x) * x * mEnergy + x * x) + 1;
    }
}