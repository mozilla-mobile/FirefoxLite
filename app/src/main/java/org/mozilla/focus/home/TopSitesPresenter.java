/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import androidx.annotation.NonNull;

import org.mozilla.focus.history.model.Site;

import java.util.ArrayList;
import java.util.List;

public class TopSitesPresenter implements TopSitesContract.Presenter {

    private TopSitesContract.View view;

    private List<Site> sites;

    public TopSitesPresenter() {
        this.sites = new ArrayList<>();
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

    @Override
    public void setSites(List<Site> sites) {
        if (sites != null) {
            this.sites = sites;
        } else {
            this.sites = new ArrayList<>();
        }
    }

    @Override
    public List<Site> getSites() {
        return this.sites;
    }
}
