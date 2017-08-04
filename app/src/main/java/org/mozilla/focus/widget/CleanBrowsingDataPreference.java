package org.mozilla.focus.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

import org.mozilla.focus.R;

/**
 * Created by ylai on 2017/8/3.
 */

public class CleanBrowsingDataPreference extends MultiSelectListPreference {
    public CleanBrowsingDataPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CleanBrowsingDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        buildList();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null);
        builder.setPositiveButton(getContext().getResources().getString(R.string.setting_dialog_clear_data), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                /*
                 *   Clear browsing data Callback function
                 *   Using getValuse() to know user select functions
                 */


                //Called when the dialog is dismissed and should be used to save data to the SharedPreferences.
                onDialogClosed(true);
                dialogInterface.dismiss();
            }
        });

        builder.setNegativeButton(getContext().getResources().getString(R.string.setting_dialog_cancel), this);
    }

    private void buildList()
    {
        final String[] entries = getContext().getResources().getStringArray(R.array.clean_browsing_data_entries);
        final String[] values = getContext().getResources().getStringArray(R.array.clean_browsing_data_values);

        setEntries(entries);
        setEntryValues(values);
    }
}
