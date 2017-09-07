/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.fragment;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.firstrun.FirstrunPagerAdapter;
import org.mozilla.focus.telemetry.TelemetryWrapper;

public class FirstrunFragment extends Fragment implements View.OnClickListener {
    public static final String FRAGMENT_TAG = "firstrun";

    public static final String FIRSTRUN_PREF = "firstrun_shown";

    public static FirstrunFragment create() {
        return new FirstrunFragment();
    }

    private ViewPager viewPager;

    private TransitionDrawable bgTransitionDrawable;
    private Drawable[] bgDrawables;

    private boolean isTelemetryValid = true;
    private long telemetryStartTimestamp = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDrawables();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final Transition transition = TransitionInflater.from(context).
                inflateTransition(R.transition.firstrun_exit);

        setExitTransition(transition);

        // We will send a telemetry event whenever a new firstrun page is shown. However this page
        // listener won't fire for the initial page we are showing. So we are going to firing here.
        isTelemetryValid = true;
        telemetryStartTimestamp = System.currentTimeMillis();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_firstrun, container, false);

        view.findViewById(R.id.skip).setOnClickListener(this);

        final View background = view.findViewById(R.id.background);
        background.setBackground(bgTransitionDrawable);

        final FirstrunPagerAdapter adapter = new FirstrunPagerAdapter(container.getContext(), this);

        viewPager = (ViewPager) view.findViewById(R.id.pager);

        viewPager.setPageTransformer(true, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View page, float position) {
                page.setAlpha(1 - (0.5f * Math.abs(position)));
            }
        });

        viewPager.setClipToPadding(false);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int newIdx) {
                final int duration = 400;
                final Drawable nextDrawable = bgDrawables[newIdx % bgDrawables.length];

                if ((newIdx % 2) == 0) {
                    // next page is even number
                    bgTransitionDrawable.setDrawableByLayerId(R.id.first_run_bg_even, nextDrawable);
                    bgTransitionDrawable.reverseTransition(duration); // odd -> even
                } else {
                    // next page is odd number
                    bgTransitionDrawable.setDrawableByLayerId(R.id.first_run_bg_odd, nextDrawable);
                    bgTransitionDrawable.startTransition(duration); // even -> odd
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        final TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager, true);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        isTelemetryValid = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next:
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                break;

            case R.id.skip:
                finishFirstrun();
                break;

            case R.id.finish:
                finishFirstrun();
                if(isTelemetryValid) {
                    TelemetryWrapper.finishFirstRunEvent(System.currentTimeMillis() - telemetryStartTimestamp);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown view");
        }
    }

    private void finishFirstrun() {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putBoolean(FIRSTRUN_PREF, true)
                .apply();

        ((MainActivity) getActivity()).firstrunFinished();
    }

    // FirstRun fragment is not used often, so we create drawables programmatically, instead of add
    // lots of drawable resources
    private void initDrawables() {
        final GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TR_BL;
        bgDrawables = new Drawable[]{
                new GradientDrawable(orientation, new int[]{0xFF75ADB3, 0xFF328BD1}),
                new GradientDrawable(orientation, new int[]{0xFF7EBCB5, 0xFF328BD1}),
                new GradientDrawable(orientation, new int[]{0xFF83C8B3, 0xFF328BD1}),
                new GradientDrawable(orientation, new int[]{0xFF8ED8B3, 0xFF328BD1}),
        };

        bgTransitionDrawable = new TransitionDrawable(bgDrawables);
        bgTransitionDrawable.setId(0, R.id.first_run_bg_even);
        bgTransitionDrawable.setId(1, R.id.first_run_bg_odd);
    }

}
