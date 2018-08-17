/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import org.mozilla.focus.history.model.Site;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by ylai on 2017/8/16.
 */

public class TopSiteComparator implements Comparator<Site>, Serializable {
    @Override
    public int compare(Site site1, Site site2) {
        long viewCount1 = site1.getViewCount();
        long viewCount2 = site2.getViewCount();

        if (viewCount1 > viewCount2) {
            return -1;
        } else if (viewCount1 < viewCount2) {
            return 1;
        } else {
            long lastViewTime1 = site1.getLastViewTimestamp();
            long lastViewTime2 = site2.getLastViewTimestamp();
            if (lastViewTime1 > lastViewTime2) {
                return -1;
            } else if (lastViewTime1 < lastViewTime2) {
                return 1;
            }
            return 0;
        }
    }
}
