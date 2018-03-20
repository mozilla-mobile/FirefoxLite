/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.firstrun;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.OnCompositionLoadedListener;

import org.mozilla.focus.R;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.Settings;

public class FirstrunPagerAdapter extends PagerAdapter {

    private final static int TURBO_MODE_PAGE_INDEX = 1;

    private static class FirstrunPage {
        public final String title;
        public final String text;
        public final String imageResource;

        private FirstrunPage(String title, String text, String json) {
            this.title = title;
            this.text = text;
            this.imageResource = json;
        }
    }

    private final FirstrunPage[] pages;

    private Context context;
    private View.OnClickListener listener;

    public FirstrunPagerAdapter(Context context, View.OnClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.pages = new FirstrunPage[]{
                new FirstrunPage(
                        context.getString(R.string.first_run_page2_title),
                        context.getString(R.string.first_run_page2_text),
                        "first_run_img_2.json"),
                new FirstrunPage(
                        context.getString(R.string.first_run_page3_title),
                        context.getString(R.string.first_run_page3_text),
                        "first_run_img_3.json"),
                new FirstrunPage(
                        context.getString(R.string.first_run_page4_title),
                        context.getString(R.string.first_run_page4_text),
                        "first_run_img_4.json"),
                new FirstrunPage(
                        context.getString(R.string.first_run_page1_title, context.getString(R.string.app_name)),
                        context.getString(R.string.first_run_page1_text),
                        "first_run_img_1.json")
        };
    }

    private View getView(int position, ViewPager pager) {
        final View view = LayoutInflater.from(context).inflate(
                R.layout.firstrun_page, pager, false);

        final FirstrunPage page = pages[position];

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText(page.title);

        final TextView textView = (TextView) view.findViewById(R.id.text);
        textView.setText(page.text);

        final ImageView imageView = (ImageView) view.findViewById(R.id.image);
        final LottieDrawable drawable = new LottieDrawable();
        LottieComposition.Factory.fromAssetFileName(context,
                page.imageResource,
                new OnCompositionLoadedListener() {
                    @Override
                    public void onCompositionLoaded(@Nullable LottieComposition composition) {
                        drawable.setComposition(composition);
                    }
                });
        imageView.setImageDrawable(drawable);

        final Button buttonView = (Button) view.findViewById(R.id.button);
        buttonView.setOnClickListener(listener);
        if (position == pages.length - 1) {
            buttonView.setText(R.string.firstrun_close_button);
            buttonView.setId(R.id.finish);
        } else {
            buttonView.setText(R.string.firstrun_next_button);
            buttonView.setId(R.id.next);
        }

        if (position == TURBO_MODE_PAGE_INDEX) {
            initForTurboModePage(context, view);
        }

        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getCount() {
        return pages.length;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ViewPager pager = (ViewPager) container;
        View view = getView(position, pager);

        pager.addView(view);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }

    private void initForTurboModePage(@NonNull Context context, @NonNull final View group) {
        final Switch widget = (Switch) group.findViewById(R.id.switch_widget);
        final Settings settings = Settings.getInstance(context);
        widget.setVisibility(View.VISIBLE);
        widget.setText(R.string.label_menu_turbo_mode);
        widget.setChecked(settings.shouldUseTurboMode());
        widget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setTurboMode(isChecked);
                TelemetryWrapper.toggleFirstRunPageEvent(isChecked);
            }
        });
    }
}
