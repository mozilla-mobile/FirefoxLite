/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.mozilla.focus.Inject;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.Settings;

public class BeforeTestTask {
    private boolean enableRateAppPromotion;
    private boolean skipFirstRun;

    public BeforeTestTask(Builder builder) {
        this.enableRateAppPromotion = builder.enableRateAppPromotion;
        this.skipFirstRun = builder.skipFirstRun;
    }

    public void execute() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (context == null) {
            return;
        }

        if (this.skipFirstRun) {
            NewFeatureNotice.getInstance(context).setMultiTabUpdateNoticeDidShow();
        } else {
            NewFeatureNotice.getInstance(context).resetFirstRunDidShow();
        }
        final Settings settings = Settings.getInstance(context);
        if (settings != null) {
            settings.setShareAppDialogDidShow();
            if (!this.enableRateAppPromotion) {
                settings.setRateAppDialogDidShow();
            }
        }

        Inject.getTabsDatabase(null).tabDao().deleteAllTabs();
        AndroidTestUtils.setFocusTabId("");
    }


    public static class Builder {

        private boolean enableRateAppPromotion;
        private boolean skipFirstRun;

        public Builder() {
            this.enableRateAppPromotion = false;
            this.skipFirstRun = true;
        }

        public Builder setRateAppPromotionEnabled(boolean enable) {
            this.enableRateAppPromotion = enable;
            return this;
        }

        public Builder setSkipFirstRun(boolean skipFirstRun) {
            this.skipFirstRun = skipFirstRun;
            return this;
        }

        public BeforeTestTask build() {
            return new BeforeTestTask(this);
        }
    }
}
