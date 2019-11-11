/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.InstrumentationRegistry;

import org.mozilla.focus.FocusApplication;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.persistence.BookmarksDatabase;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.Settings;

public class BeforeTestTask {
    private boolean enableRateAppPromotion;
    private boolean skipFirstRun;
    private boolean skipHomePageOnboarding;
    private boolean skipHomeShoppingSearchOnboarding;
    private boolean clearBrowsingHistory;
    private boolean enableSreenshotOnBoarding;
    private boolean enableDownloadIndicatorIntro;

    public BeforeTestTask(Builder builder) {
        this.enableRateAppPromotion = builder.enableRateAppPromotion;
        this.skipFirstRun = builder.skipFirstRun;
        this.skipHomePageOnboarding = builder.skipHomePageOnboarding;
        this.skipHomeShoppingSearchOnboarding = builder.skipHomeShoppingSearchOnbaording;
        this.clearBrowsingHistory = builder.clearBrowsingHistory;
        this.enableSreenshotOnBoarding = builder.enableSreenshotOnBoarding;
        this.enableDownloadIndicatorIntro = builder.enableDownloadIndicatorIntro;
    }

    public void execute() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (context == null) {
            return;
        }

        ((FocusApplication) context.getApplicationContext()).resetAppComponent();
        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPref.edit().clear().commit();

        if (this.skipFirstRun) {
            NewFeatureNotice.getInstance(context).setLiteUpdateDidShow();
        } else {
            NewFeatureNotice.getInstance(context).resetFirstRunDidShow();
        }
        if (this.skipHomePageOnboarding) {
            NewFeatureNotice.getInstance(context).setHomePageOnboardingDidShow();
        } else {
            NewFeatureNotice.getInstance(context).resetHomePageOnboardingDidShow();
        }
        if (this.skipHomeShoppingSearchOnboarding) {
            NewFeatureNotice.getInstance(context).setHomeShoppingSearchOnboardingDidShow();
        } else {
            NewFeatureNotice.getInstance(context).resetHomeShoppingSearchOnboardingDidShow();
        }
        final Settings settings = Settings.getInstance(context);
        if (settings != null) {
            settings.setShareAppDialogDidShow();
            if (!this.enableRateAppPromotion) {
                settings.setRateAppDialogDidShow();
            }
            // disable screenshot on boarding
            if (!this.enableSreenshotOnBoarding) {
                settings.getEventHistory().add(Settings.Event.ShowMyShotOnBoardingDialog);
            }
            // disable download indicator intro
            if (!this.enableDownloadIndicatorIntro) {
                settings.getEventHistory().add(Settings.Event.ShowDownloadIndicatorIntro);
            }
        }
        if (this.clearBrowsingHistory) {
            //TODO: should consider using IdlingResource for DB operation or in-memory DB
            BrowsingHistoryManager.getInstance().deleteAll(null);
        }

        BookmarksDatabase.getInstance(context).bookmarkDao().deleteAllBookmarks();
        AndroidTestUtils.setFocusTabId("");
        // Disable privacy update notice
        NewFeatureNotice.getInstance(context).setPrivacyPolicyUpdateNoticeDidShow();
    }


    public static class Builder {

        private boolean enableSreenshotOnBoarding;
        private boolean enableDownloadIndicatorIntro;
        private boolean enableRateAppPromotion;
        private boolean skipFirstRun;
        private boolean skipHomePageOnboarding;
        private boolean skipHomeShoppingSearchOnbaording;
        private boolean clearBrowsingHistory;

        public Builder() {
            this.enableRateAppPromotion = false;
            this.skipFirstRun = true;
            this.skipHomePageOnboarding = true;
            this.skipHomeShoppingSearchOnbaording = true;
            this.clearBrowsingHistory = false;
            this.enableSreenshotOnBoarding = false;
            this.enableDownloadIndicatorIntro = false;
        }

        public Builder setRateAppPromotionEnabled(boolean enable) {
            this.enableRateAppPromotion = enable;
            return this;
        }

        public Builder setSkipFirstRun(boolean skipFirstRun) {
            this.skipFirstRun = skipFirstRun;
            return this;
        }

        public Builder setSkipHomePageOnboarding(boolean skipHomePageOnboarding) {
            this.skipHomePageOnboarding = skipHomePageOnboarding;
            return this;
        }

        public Builder setSkipShoppingSearchOnboarding(boolean skipHomeShoppingSearchOnbarding) {
            this.skipHomeShoppingSearchOnbaording = skipHomeShoppingSearchOnbarding;
            return this;
        }

        public Builder clearBrowsingHistory() {
            this.clearBrowsingHistory = true;
            return this;
        }

        public Builder enableSreenshotOnBoarding(boolean enableSreenshotOnBoarding) {
            this.enableSreenshotOnBoarding = enableSreenshotOnBoarding;
            return this;
        }

        public Builder enableDownloadIndicatorIntro(boolean enableDownloadIndicatorIntro) {
            this.enableDownloadIndicatorIntro = enableDownloadIndicatorIntro;
            return this;
        }

        public BeforeTestTask build() {
            return new BeforeTestTask(this);
        }
    }
}
