package org.mozilla.focus.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebViewDatabase;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.TopSitesUtils;

import java.util.Set;

/**
 * Created by ylai on 2017/8/3.
 */

public class CleanBrowsingDataPreference extends MultiSelectListPreference {

    public CleanBrowsingDataPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CleanBrowsingDataPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Resources resources = getContext().getResources();
            //  On click positive callback here get current value by getValues();
            for(String value : getValues()) {
                if (resources.getString(R.string.pref_value_clear_browsing_history).equals(value)) {
                    BrowsingHistoryManager.getInstance().deleteAll(null);
                    TopSitesUtils.getDefaultSitesJsonArrayFromAssets(getContext());
                } else if (resources.getString(R.string.pref_value_clear_cookies).equals(value)) {
                    CookieManager.getInstance().removeAllCookies(null);
                } else if (resources.getString(R.string.pref_value_clear_cache).equals(value)) {
                    //  TODO: Clear Cache
                } else if (resources.getString(R.string.pref_value_clear_form_history).equals(value)){
                    WebViewDatabase.getInstance(getContext()).clearFormData();
                }
            }

            if (getValues().size() > 0) {
                Toast.makeText(getContext(), R.string.message_cleared_browsing_data, Toast.LENGTH_SHORT).show();
            }
            TelemetryWrapper.settingsEvent(getKey(), flattenToJsonObject(getValues()).toString());
        }
    }

    private Object flattenToJsonObject(Set<String> values) {
        final JSONObject object = new JSONObject();

        final String[] preferenceKeys = getContext().getResources().getStringArray(R.array.clean_browsing_data_values);
        if (preferenceKeys.length <= 0) {
            return object;
        }

        for (String key : preferenceKeys) {
            try {
                if (values.contains(key)) {
                    object.put(key, Boolean.TRUE.toString());
                } else {
                    object.put(key, JSONObject.NULL);
                }
            } catch (JSONException e) {
                throw new AssertionError("Preference value can't be serialized to JSON", e);
            }
        }

        return object;
    }

}
