package org.mozilla.focus.firstrun;

import android.content.Context;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.SupportUtils;

public class UpgradeFirstrunPagerAdapter extends FirstrunPagerAdapter {

    public UpgradeFirstrunPagerAdapter(Context context, View.OnClickListener listener) {
        super(context, listener);
        if (NewFeatureNotice.getInstance(context).from21to40()) {
            this.pages.add(new FirstrunPage(
                    context.getString(R.string.new_name_upgrade_page_title),
                    context.getString(R.string.new_name_upgrade_page_text, context.getString(R.string.app_name)),
                    R.drawable.ic_onboarding_first_use));
        }

        if (NewFeatureNotice.getInstance(context).from40to114() && context.getResources().getInteger(R.integer.news_portal) > 0) {
            FirstRunLibrary.buildLifeFeedFirstrun(context);
        }


    }
}
