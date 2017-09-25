package org.mozilla.focus.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;

/**
 * Created by ylai on 2017/9/21.
 */

public class TurboSwitchPreference extends Preference {

    public TurboSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TurboSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_turbo);

        // We are keeping track of the preference value ourselves.
        setPersistent(false);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final Switch switchWidget = (Switch) view.findViewById(R.id.switch_widget);

        switchWidget.setChecked(Settings.getInstance(getContext()).shouldUseTurboMode());

        switchWidget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.getInstance(getContext()).setTurboMode(isChecked);
            }
        });

        final TextView summary = (TextView) view.findViewById(android.R.id.summary);

        TypedValue typedValue = new TypedValue();
        TypedArray ta = getContext().obtainStyledAttributes(typedValue.data, new int[] { android.R.attr.textColorLink });
        int color = ta.getColor(0, 0);
        ta.recycle();

        summary.setTextColor(color);
        summary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is a hardcoded link: if we ever end up needing more of these links, we should
                // move the link into an xml parameter, but there's no advantage to making it configurable now.
                final String url = SupportUtils.getSumoURLForTopic(getContext(), "turbo");
                final String title = getTitle().toString();

                final Intent intent = InfoActivity.getIntentFor(getContext(), url, title);
                getContext().startActivity(intent);
            }
        });

        // We still want to allow toggling the pref by touching any part of the pref (except for
        // the "learn more" link)
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                switchWidget.toggle();
                return true;
            }
        });
    }
}
