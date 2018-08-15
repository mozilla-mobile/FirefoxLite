/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.mozilla.focus.Inject;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.persistence.BookmarksDatabase;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.Settings;
import org.mozilla.rocket.theme.ThemeManager;

public class BeforeTestTask {
    private boolean enableRateAppPromotion;
    private boolean skipFirstRun;
    private boolean clearBrowsingHistory;

    public BeforeTestTask(Builder builder) {
        this.enableRateAppPromotion = builder.enableRateAppPromotion;
        this.skipFirstRun = builder.skipFirstRun;
        this.clearBrowsingHistory = builder.clearBrowsingHistory;
    }

    public void execute() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (context == null) {
            return;
        }

        if (this.skipFirstRun) {
            NewFeatureNotice.getInstance(context).setMultiTabUpdateNoticeDidShow();
            ThemeManager.dismissOnboarding(context);
        } else {
            NewFeatureNotice.getInstance(context).resetFirstRunDidShow();
        }
        final Settings settings = Settings.getInstance(context);
        if (settings != null) {
            settings.setShareAppDialogDidShow();
            if (!this.enableRateAppPromotion) {
                settings.setRateAppDialogDidShow();
            }
            // disable screenshot on boarding
            settings.getEventHistory().add(Settings.Event.ShowMyShotOnBoardingDialog);;
        }
        if (this.clearBrowsingHistory) {
            //TODO: should consider using IdlingResource for DB operation or in-memory DB
            BrowsingHistoryManager.getInstance().deleteAll(null);
        }

        Inject.getTabsDatabase(null).tabDao().deleteAllTabs();
        BookmarksDatabase.getInstance(context).bookmarkDao().deleteAllBookmarks();
        AndroidTestUtils.setFocusTabId("");
        // Disable theme on boarding
        ThemeManager.dismissOnboarding(context);
    }


    public static class Builder {

        private boolean enableRateAppPromotion;
        private boolean skipFirstRun;
        private boolean clearBrowsingHistory;

        public Builder() {
            this.enableRateAppPromotion = false;
            this.skipFirstRun = true;
            this.clearBrowsingHistory = false;
        }

        public Builder setRateAppPromotionEnabled(boolean enable) {
            this.enableRateAppPromotion = enable;
            return this;
        }

        public Builder setSkipFirstRun(boolean skipFirstRun) {
            this.skipFirstRun = skipFirstRun;
            return this;
        }

        public Builder clearBrowsingHistory() {
            this.clearBrowsingHistory = true;
            return this;
        }

        public BeforeTestTask build() {
            return new BeforeTestTask(this);
        }
    }
}
