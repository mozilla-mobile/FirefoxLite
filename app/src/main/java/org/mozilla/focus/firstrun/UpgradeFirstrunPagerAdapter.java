package org.mozilla.focus.firstrun;

import android.content.Context;
import android.view.View;

import org.mozilla.focus.R;

public class UpgradeFirstrunPagerAdapter extends FirstrunPagerAdapter {

    public UpgradeFirstrunPagerAdapter(Context context, View.OnClickListener listener) {
        super(context, listener);
        this.pages = new FirstrunPage[]{
                new FirstrunPage(
                        context.getString(R.string.new_name_upgrade_page_title),
                        context.getString(R.string.new_name_upgrade_page_text, context.getString(R.string.app_name)),
                        R.drawable.ic_onboarding_first_use),
        };
    }
}
