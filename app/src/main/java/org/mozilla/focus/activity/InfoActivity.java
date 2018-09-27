/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.InfoFragment;
import org.mozilla.focus.locale.Locales;
import org.mozilla.focus.utils.SupportUtils;

/**
 * A generic activity that supports showing additional information in a WebView. This is useful
 * for showing any web based content, including About/Help/Rights, and also SUMO pages.
 */
public class InfoActivity extends AppCompatActivity {
    private static final String EXTRA_URL = "extra_url";
    private static final String EXTRA_TITLE = "extra_title";
    private InfoFragment infoFragment;

    public static final Intent getIntentFor(final Context context, final String url, final String title) {
        final Intent intent = new Intent(context, InfoActivity.class);

        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);

        return intent;
    }

    public static final Intent getAboutIntent(final Context context) {
        final Resources resources = Locales.getLocalizedResources(context);

        // We can't use "about:" because webview silently swallows about: pages, hence we use
        // a custom scheme.
        return getIntentFor(context, SupportUtils.FOCUS_ABOUT_URL, resources.getString(R.string.menu_about));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_info);

        final String url = getIntent().getStringExtra(EXTRA_URL);
        final String title = getIntent().getStringExtra(EXTRA_TITLE);

        infoFragment = InfoFragment.create(url);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.infofragment, infoFragment)
                .commit();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (infoFragment != null && infoFragment.canGoBack()) {
            infoFragment.goBack();
            return;
        }
        super.onBackPressed();
    }
}
