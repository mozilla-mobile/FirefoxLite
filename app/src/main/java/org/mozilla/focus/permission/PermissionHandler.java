package org.mozilla.focus.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.IntentUtils;

import java.io.Serializable;

public class PermissionHandler {

    private static final int REQUEST_SETTINGS = 39528;
    private static final String PERMISSION_KEY = "HANDLER_PERMISSION_KEY";
    private static final String ACTION_ID_KEY = "HANDLER_ACTION_ID_KEY";
    private static final String PARAMS_KEY = "HANDLER_PARAMS_KEY";
    private static final String PERMISSION_PREFIX = "PERM_";


    private PermissionHandle permissionHandle;
    private String permission;
    private int actionId;
    private Serializable params;

    public PermissionHandler(PermissionHandle permissionHandle) {
        this.permissionHandle = permissionHandle;
    }

    public void tryAction(final Activity activity, final String permission, final int actionId, final Serializable params) {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission)) {
            // We do have the permission.
            permissionHandle.doAction(permission, actionId, PermissionHandle.TRIGGER_DIRECT, params);
        } else {
            // We do not have the permission to write to the external storage. Request the permission and start the
            // capture from onRequestPermissionsResult().
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setAction(permission, actionId, params);
                // First permission ask, Never ask me again or not able to grand the permission
                if (!isFirstTimeAsking(activity, permission) && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // TODO: 1/3/18
                    // This will also be shown when the device is not able to grant the permission
                    // We might want to deal with this at some point?
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(permissionHandle.getDoNotAskAgainDialogString(actionId))
                            .setCancelable(true)
                            .setPositiveButton(R.string.permission_dialog_setting, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    IntentUtils.intentOpenSettings(activity, REQUEST_SETTINGS);
                                }
                            })
                            .setNegativeButton(R.string.permission_dialog_not_now, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    clearAction();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    clearAction();
                                }
                            });
                    builder.show();
                } else {
                    permissionHandle.requestPermissions(actionId);
                }
            }
        }
    }

    private boolean isFirstTimeAsking(Context context, String permission) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = PERMISSION_PREFIX + permission;
        boolean exist = preferences.contains(key);
        preferences.edit().putBoolean(key, true).apply();
        return !exist;
    }

    private void setAction(String permission, int actionId, Serializable params) {
        this.permission = permission;
        this.actionId = actionId;
        this.params = params;
    }

    private void clearAction() {
        setAction(null, -1, null);
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS && actionId != -1) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission)) {
                permissionHandle.doAction(permission, actionId, PermissionHandle.TRIGGER_SETTING, params);
            }
        }
        clearAction();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == actionId) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionHandle.doAction(permission, actionId, PermissionHandle.TRIGGER_GRANTED, params);
                clearAction();
            } else {
                Snackbar snackbar = permissionHandle.makeAskAgainSnackBar(actionId);
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, @DismissEvent int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            clearAction();
                        }
                    }
                });
                snackbar.show();
            }
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        permission = savedInstanceState.getString(PERMISSION_KEY);
        actionId = savedInstanceState.getInt(ACTION_ID_KEY);
        params = savedInstanceState.getSerializable(PARAMS_KEY);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(PERMISSION_KEY, permission);
        outState.putInt(ACTION_ID_KEY, actionId);
        outState.putSerializable(PARAMS_KEY, params);
    }

    public static Snackbar makeAskAgainSnackBar(final Activity activity, final View view, final int stringId) {
        return Snackbar.make(view, stringId, Snackbar.LENGTH_LONG)
                .setAction(R.string.permission_dialog_setting, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        IntentUtils.intentOpenSettings(activity, REQUEST_SETTINGS);
                    }
                });
    }
}
