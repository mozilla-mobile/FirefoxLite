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
    }

    @Override
    public void setView(@NonNull TopSitesContract.View view) {
        this.view = view;
    }

    @Override
    public void populateSites() {
        if (this.view != null) {
            this.view.showSites(this.sites);
        }
    }

    @Override
    public void addSite(@NonNull Site site) {
        this.sites.add(site);
        if (this.view != null) {
            this.view.appendSite(site);
        }
    }

    @Override
    public void removeSite(@NonNull Site site) {
        this.sites.remove(site);
        if (this.view != null) {
            this.view.removeSite(site);
        }
    }

    /**
     * FIXME: should remove this method
     * A method to insert fake data.
     */
    private void initSites() {
        final String[] urls = {
                "https://www.amazon.com/",
                "https://duckduckgo.com/",
                "https://vimeo.com/",
                "https://mbasic.facebook.com/",
                "https://www.google.com",
                "https://twitter.com",
                "https://www.mozilla.org",
                "https://developer.mozilla.org/zh-TW/"
        };

        final String[] titles = {
                "Amazon",
                "DuckDuckGo",
                "Vimeo",
                "Facebook",
                "Google",
                "Twitter",
                "Mozilla",
                "MDN"
        };

        final int[] icons = {
                R.mipmap.ic_amazon,
                R.mipmap.ic_duckduckgo,
                R.mipmap.ic_vimeo,
                R.mipmap.ic_facebook,
                R.mipmap.ic_google,
                R.mipmap.ic_twitter,
                R.mipmap.ic_mozilla,
                R.mipmap.ic_mdn
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
