package org.mozilla.focus.permission;

import android.support.design.widget.Snackbar;

import java.io.Serializable;

public interface PermissionHandle {

    int TRIGGER_DIRECT = 0;
    int TRIGGER_GRANTED = 1;
    int TRIGGER_SETTING = 2;

    /**
     * List of action implementations that requires a permission that is used in this
     * Activity/Fragment. Note that Activity does my be destroyed so be aware to test
     * with ALWAYS_CLOSE_ACTIVITY enabled in developer options.
     *
     * @param  permission the required permission
     * @param  actionId the designated action
     * @param  triggerType the source that triggers doAction
     * @param  params the optional params that is used in this action
     */
    void doAction(String permission, int actionId, int triggerType, Serializable params);

    /**
     * A mapping of used string that is used in the DoNotAskAgainDialog
     *
     * @param  actionId the designated action
     * @return      the string id of the message
     */
    int getDoNotAskAgainDialogString(int actionId);

    /**
     * The generation of the AskAgainSnackBar a Bar is returned instead of String since we need to
     * know which view to insert and we want to add callbacks in {@link PermissionHandler}
     *
     * @param  actionId the designated action
     * @return      a {@link Snackbar} instance which can be showed directly
     */
    Snackbar makeAskAgainSnackBar(int actionId);

    /**
     * Try to Request the corresponding permission to this actionId
     *
     * @param  actionId the designated action
     */
    void requestPermissions(int actionId);
}
