package org.mozilla.focus.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.util.Log;

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
            //  On click positive callback here get current value by getValues();
        }
    }
}
