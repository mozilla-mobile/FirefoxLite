package org.mozilla.focus.firstrun;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Switch;

import org.mozilla.focus.R;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.NewFeatureNotice;
import org.mozilla.focus.utils.Settings;

public class DefaultFirstrunPagerAdapter extends FirstrunPagerAdapter {

    private final static int TURBO_MODE_PAGE_INDEX = 0;

    public DefaultFirstrunPagerAdapter(Context context, View.OnClickListener listener) {
        super(context, listener);

        this.pages.add(new FirstrunPage(
                context.getString(R.string.first_run_page2_title),
                context.getString(R.string.first_run_page2_text),
                "first_run_img_2.json"));
        this.pages.add(new FirstrunPage(
                context.getString(R.string.first_run_page4_title),
                context.getString(R.string.first_run_page4_text),
                "first_run_img_4.json"));
        this.pages.add(new FirstrunPage(
                context.getString(R.string.first_run_page5_title),
                context.getString(R.string.first_run_page5_text),
                R.drawable.ic_onboarding_privacy));
        this.pages.add(new FirstrunPage(
                context.getString(R.string.first_run_page3_title),
                context.getString(R.string.first_run_page3_text),
                "first_run_img_3.json"));

        final NewFeatureNotice featureNotice = NewFeatureNotice.getInstance(context);
        final boolean shouldShowShoppingLink = featureNotice.shouldShowEcShoppingLinkOnboarding();

        if (AppConfigWrapper.hasNewsPortal() || shouldShowShoppingLink) {
            featureNotice.hasShownEcShoppingLink();
            this.pages.add(FirstRunLibrary.buildLifeFeedFirstrun(context));
        }
    }

    @Override
    protected View getView(int position, ViewPager pager) {
        View v = super.getView(position, pager);

        if (position == TURBO_MODE_PAGE_INDEX) {
            initForTurboModePage(context, v);
        }
        return v;
    }


    private void initForTurboModePage(@NonNull Context context, @NonNull final View group) {
        final Switch widget = group.findViewById(R.id.switch_widget);
        final Settings settings = Settings.getInstance(context);
        widget.setVisibility(View.VISIBLE);
        widget.setText(R.string.label_menu_turbo_mode);
        widget.setChecked(settings.shouldUseTurboMode());
        widget.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setTurboMode(isChecked);
            TelemetryWrapper.toggleFirstRunPageEvent(isChecked);
        });
    }
}
