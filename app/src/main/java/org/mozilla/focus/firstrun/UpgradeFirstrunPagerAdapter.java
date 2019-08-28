package org.mozilla.focus.firstrun;

import android.content.Context;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.rocket.home.topsites.data.PinSiteManager;
import org.mozilla.rocket.home.topsites.data.PinSiteManagerKt;

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

        // TODO: To be removed, force to disable pin site tutorial
//        PinSiteManager pinSiteManager = PinSiteManagerKt.getPinSiteManager(context);
//        if (pinSiteManager.isEnabled() && pinSiteManager.isFirstTimeEnable()) {
//            this.pages.add(new FirstrunPage(
//                    context.getString(R.string.second_run_upgrade_page_title),
//                    context.getString(R.string.second_run_upgrade_page_text),
//                    R.drawable.ic_onboarding_pinsites
//            ));
//        }
    }
}
