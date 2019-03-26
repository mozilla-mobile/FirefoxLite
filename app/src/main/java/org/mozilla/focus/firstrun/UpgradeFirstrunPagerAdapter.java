package org.mozilla.focus.firstrun;

import android.content.Context;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.rocket.home.pinsite.PinSiteManager;
import org.mozilla.rocket.home.pinsite.PinSiteManagerKt;

public class UpgradeFirstrunPagerAdapter extends FirstrunPagerAdapter {

    public UpgradeFirstrunPagerAdapter(Context context, View.OnClickListener listener) {
        super(context, listener);
        final NewFeatureNotice featureNotice = NewFeatureNotice.getInstance(context);

        if (featureNotice.from21to40()) {
            this.pages.add(new FirstrunPage(
                    context.getString(R.string.new_name_upgrade_page_title),
                    context.getString(R.string.new_name_upgrade_page_text, context.getString(R.string.app_name)),
                    R.drawable.ic_onboarding_first_use));
        }

        final boolean shouldShowNews = featureNotice.from40to114() && AppConfigWrapper.hasNewsPortal();
        final boolean shouldShowShoppingLink = featureNotice.shouldShowEcShoppingLinkOnboarding();
        if (shouldShowNews || shouldShowShoppingLink) {
            featureNotice.hasShownEcShoppingLink();
            this.pages.add(FirstRunLibrary.buildLifeFeedFirstrun(context));
        }

        PinSiteManager pinSiteManager = PinSiteManagerKt.getPinSiteManager(context);
        if (pinSiteManager.isEnabled() && pinSiteManager.isFirstTimeEnable()) {
            this.pages.add(new FirstrunPage(
                    context.getString(R.string.second_run_upgrade_page_title),
                    context.getString(R.string.second_run_upgrade_page_text),
                    R.drawable.ic_onboarding_pinsites
            ));
        }
    }
}
