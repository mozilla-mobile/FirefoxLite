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
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.IntentUtils;

public class PermissionHandler {

    private static final int REQUEST_SETTINGS = 39528;
    private static final String PERMISSION_KEY = "HANDLER_PERMISSION_KEY";
    private static final String ACTION_ID_KEY = "HANDLER_ACTION_ID_KEY";
    private static final String PARAMS_KEY = "HANDLER_PARAMS_KEY";
    private static final String PERMISSION_PREFIX = "PERM_";
    private static final int NO_ACTION = -1;


    private PermissionHandle permissionHandle;
    private String permission;
    private int actionId = NO_ACTION;
    private Parcelable params;

    public PermissionHandler(PermissionHandle permissionHandle) {
        this.permissionHandle = permissionHandle;
    }

    public void tryAction(final Activity activity, final String permission, final int actionId, final Parcelable params) {
        tryAction(activity, permission, actionId, params, ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                IntentUtils.intentOpenSettings(activity, REQUEST_SETTINGS);
            }
        });
    }

    public void tryAction(final Fragment fragment, final String permission, final int actionId, final Parcelable params) {
        final Activity activity = fragment.getActivity();
        tryAction(activity, permission, actionId, params, fragment.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                IntentUtils.intentOpenSettings(fragment, REQUEST_SETTINGS);
            }
        });
    }

    private void tryAction(final Activity activity, final String permission, final int actionId, final Parcelable params, final boolean shouldShowRequestPermissionRationale, final DialogInterface.OnClickListener launchSetting) {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission)) {
            // We do have the permission.
            permissionHandle.doActionDirect(permission, actionId, params);
        } else {
            // We do not have the permission to write to the external storage. Request the permission and start the
            // capture from onRequestPermissionsResult().
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setAction(permission, actionId, params);
                // First permission ask, Never ask me again or not able to grand the permission
                if (!isFirstTimeAsking(activity, permission) && !shouldShowRequestPermissionRationale) {
                    // TODO: 1/3/18
                    // This will also be shown when the device is not able to grant the permission
                    // We might want to deal with this at some point?
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(permissionHandle.getDoNotAskAgainDialogString(actionId))
                            .setCancelable(true)
                            .setPositiveButton(R.string.permission_dialog_setting, launchSetting)
                            .setNegativeButton(R.string.permission_dialog_not_now, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    permissionNotGranted();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    permissionNotGranted();
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

    private void setAction(String permission, int actionId, Parcelable params) {
        this.permission = permission;
        this.actionId = actionId;
        this.params = params;
    }

    private void permissionNotGranted() {
        permissionHandle.doActionNoPermission(permission, actionId, params);
        clearAction();
    }

    private void clearAction() {
        setAction(null, NO_ACTION, null);
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS && actionId != NO_ACTION) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission)) {
                permissionHandle.doActionSetting(permission, actionId, params);
                clearAction();
            } else {
                permissionNotGranted();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == actionId) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionHandle.doActionGranted(permission, actionId, params);
                clearAction();
            } else {
                Snackbar snackbar = permissionHandle.makeAskAgainSnackBar(actionId);
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, @DismissEvent int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            permissionNotGranted();
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
        params = savedInstanceState.getParcelable(PARAMS_KEY);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(PERMISSION_KEY, permission);
        outState.putInt(ACTION_ID_KEY, actionId);
        outState.putParcelable(PARAMS_KEY, params);
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

    public static Snackbar makeAskAgainSnackBar(final Fragment fragment, final View view, final int stringId) {
        return Snackbar.make(view, stringId, Snackbar.LENGTH_LONG)
                .setAction(R.string.permission_dialog_setting, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        IntentUtils.intentOpenSettings(fragment, REQUEST_SETTINGS);
                    }
                });
    }
}
