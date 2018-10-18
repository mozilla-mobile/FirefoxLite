package org.mozilla.focus.permission;

import android.os.Parcelable;
import android.support.design.widget.Snackbar;

public interface PermissionHandle {

    int TRIGGER_DIRECT = 0;
    int TRIGGER_GRANTED = 1;
    int TRIGGER_SETTING = 2;

    /**
     * action implementations that requires a permission that is used in this
     * Activity/Fragment. When permission is already granted, this is called.
     *
     * @param permission the required permission
     * @param actionId   the designated action
     * @param params     the optional params that is used in this action
     */
    void doActionDirect(String permission, int actionId, Parcelable params);

    /**
     * action implementations that requires a permission that is used in this
     * Activity/Fragment. When permission is not granted but requested, this is called.
     *
     * @param permission the required permission
     * @param actionId   the designated action
     * @param params     the optional params that is used in this action
     */
    void doActionGranted(String permission, int actionId, Parcelable params);

    /**
     * action implementations that requires a permission that is used in this
     * Activity/Fragment. When permission is not granted but user later tried to visit Settings,
     * this is called. Note that Activity does my be destroyed so be aware to test
     * with ALWAYS_CLOSE_ACTIVITY enabled in developer options.
     *
     * @param permission the required permission
     * @param actionId   the designated action
     * @param params     the optional params that is used in this action
     */
    void doActionSetting(String permission, int actionId, Parcelable params);

    /**
     * error handling implementations that when a required permission that is used in this
     * Activity/Fragment is requested but user chooses to not grant it.
     *
     * @param permission the required permission
     * @param actionId   the designated action
     * @param params     the optional params that is used in this action
     */
    void doActionNoPermission(String permission, int actionId, Parcelable params);

    /**
     * The generation of the AskAgainSnackBar a Bar is returned instead of String since we need to
     * know which view to insert and we want to add callbacks in {@link PermissionHandler}
     *
     * @param actionId the designated action
     * @return a {@link Snackbar} instance which can be showed directly
     */
    Snackbar makeAskAgainSnackBar(int actionId);

    /**
     * Try to Request the corresponding permission to this actionId
     *
     * @param actionId the designated action
     */
    void requestPermissions(int actionId);

    void permissionDeniedToast(int actionId);
}
