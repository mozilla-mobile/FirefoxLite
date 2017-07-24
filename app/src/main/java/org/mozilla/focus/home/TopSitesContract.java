/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.support.annotation.NonNull;

import org.mozilla.focus.home.model.Site;

import java.util.List;

public class TopSitesContract {

    interface View {
        /**
         * To display a list of sites. If there are already some sites displayed, replace them.
         *
         * @param sites list of sites to be display
         */
        void showSites(@NonNull List<Site> sites);

        /**
         * To append a site to the tail of list
         *
         * @param site to be appended
         */
        void appendSite(@NonNull Site site);

        /**
         * To remove a site from list
         *
         * @param site to be removed
         */
        void removeSite(@NonNull Site site);

        /**
         * To notify View the content of a site is changed
         *
         * @param site the changed site
         */
        void onSiteChanged(@NonNull Site site);
    }

    interface Presenter {
        /**
         * Connect this Presenter to a View
         *
         * @param view to be connected
         */
        void setView(View view);

        /**
         * To ask Presenter to get stored sites and fill into View
         */
        void populateSites();

        /**
         * To add a site into Model
         *
         * @param site to be added
         */
        void addSite(@NonNull Site site);

        /**
         * To remove a site from Model
         *
         * @param site to be removed
         */
        void removeSite(@NonNull Site site);
    }
}
