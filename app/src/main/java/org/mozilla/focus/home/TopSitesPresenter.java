/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.support.annotation.NonNull;

import org.mozilla.focus.R;
import org.mozilla.focus.home.model.Site;
import org.mozilla.focus.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

public class TopSitesPresenter implements TopSitesContract.Presenter {

    private TopSitesContract.View view;

    // FIXME: this member should be removed once we implement real Model.
    // A data structure to provide fake data to View.
    // This presenter should not retain any sites, it should get Sites from Model.
    private List<Site> sites;

    public TopSitesPresenter() {
        this.sites = new ArrayList<>();
        initSites();


        // to simulate adding a site
        ThreadUtils.postToMainThreadDelayed(new Runnable() {
            @Override
            public void run() {
                final Site site = new Site(TopSitesPresenter.this.sites.size());
                site.setIconRes(R.drawable.ic_lock);
                site.setTitle("Let's Encrypt");
                site.setUrl("https://letsencrypt.org/");
                TopSitesPresenter.this.addSite(site);
            }
        }, 2000);
    }

    @Override
    public void setView(@NonNull TopSitesContract.View view) {
        this.view = view;
    }

    @Override
    public void populateSites() {
        this.view.showSites(this.sites);
    }

    @Override
    public void addSite(@NonNull Site site) {
        this.sites.add(site);
        this.view.appendSite(site);
    }

    @Override
    public void removeSite(@NonNull Site site) {
        this.sites.remove(site);
        this.view.removeSite(site);
    }

    /**
     * FIXME: should remove this method
     * A method to insert fake data.
     */
    private void initSites() {
        final String[] urls = {
                "https://www.mozilla.org",
                "https://developer.mozilla.org/zh-TW/",
                "https://zh.wikipedia.org/wiki/Taiwan",
                "https://duckduckgo.com/",
                "https://tools.ietf.org/html/rfc2616",
                "https://www.w3.org/TR/html5/",
                "https://www.google.com",
                "https://twitter.com"
        };

        final String[] titles = {
                "Mozilla",
                "MDN",
                "Wikipedia",
                "DuckDuckGo",
                "RFC 2616",
                "HTML5",
                "Google",
                "Twitter"
        };

        final int[] icons = {
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher,
                R.mipmap.ic_launcher,
                R.drawable.ic_info,
                R.drawable.ic_shortcut_erase,
                R.drawable.ic_info,
                R.drawable.ic_shortcut_erase
        };

        for (int i = 0; i < urls.length; i++) {
            final Site site = new Site(i);
            site.setUrl(urls[i]);
            site.setTitle(titles[i]);
            site.setIconRes(icons[i]);
            this.sites.add(site);
        }
    }
}
