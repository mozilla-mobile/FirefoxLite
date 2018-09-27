/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.locale;

import androidx.fragment.app.Fragment;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.mozilla.focus.navigation.FragmentAnimationAccessor;

import java.util.Locale;

public abstract class LocaleAwareFragment extends Fragment implements FragmentAnimationAccessor {
    private Locale cachedLocale = null;
    private Animation enterTransition;
    private Animation exitTransition;

    /**
     * Is called whenever the application locale has changed. Your fragment must either update
     * all localised Strings, or replace itself with an updated version.
     */
    public abstract void applyLocale();

    @Override
    public void onResume() {
        super.onResume();

        LocaleManager.getInstance()
                .correctLocale(getContext(), getResources(), getResources().getConfiguration());

        if (cachedLocale == null) {
            cachedLocale = Locale.getDefault();
        } else {
            Locale newLocale = LocaleManager.getInstance().getCurrentLocale(getActivity().getApplicationContext());

            if (newLocale == null) {
                // Using system locale:
                newLocale = Locale.getDefault();
            }
            if (!newLocale.equals(cachedLocale)) {
                cachedLocale = newLocale;
                applyLocale();
            }
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation anim = null;
        if (nextAnim != 0) {
            anim = AnimationUtils.loadAnimation(getContext(), nextAnim);
        }

        if (enter) {
            enterTransition = anim;
        } else {
            exitTransition = anim;
        }

        return anim;
    }


    @Override
    public Animation getCustomEnterTransition() {
        return enterTransition;
    }

    @Override
    public Animation getCustomExitTransition() {
        return exitTransition;
    }
}
