package org.mozilla.focus.firstrun;

import android.content.Context;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.NewFeatureNotice;

public class UpgradeFirstrunPagerAdapter extends FirstrunPagerAdapter {

    public UpgradeFirstrunPagerAdapter(Context context, View.OnClickListener listener) {
        super(context, listener);
        final NewFeatureNotice featureNotice = NewFeatureNotice.getInstance(context);

        if (featureNotice.needToShow100To200Update()) {
            this.pages.add(new FirstrunPage(
                    context.getString(R.string.onboarding_2_introduction_title, context.getString(R.string.app_name)),
                    context.getString(R.string.onboarding_2_introduction_description),
                    R.drawable.ic_onboarding_first_use));
        }
    }
}
