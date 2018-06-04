package org.mozilla.rocket.privatebrowsing;/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.mozilla.rocket.geckopower.R;

/**
 * A generic activity that supports showing additional information in a WebView. This is useful
 * for showing any web based content, including About/Help/Rights, and also SUMO pages.
 */
public class PrivateActivity extends AppCompatActivity {
    private static final String EXTRA_URL = "extra_url";
    private WebFragment webFragment;

    public static final Intent getIntentFor(final Context context,
                                            final String url) {
        final Intent intent = new Intent(context, PrivateActivity.class);
        intent.putExtra(EXTRA_URL, url);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_private);

        final Intent intent = getIntent();
        final String url = intent.hasExtra(EXTRA_URL)
                ? intent.getStringExtra(EXTRA_URL) : "https://duckduckgo.com/";
        webFragment = WebFragment.create(url);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.private_fragment, webFragment)
                .commit();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(url);

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
        if (webFragment != null && webFragment.canGoBack()) {
            webFragment.goBack();
            return;
        }
        super.onBackPressed();
    }
}
