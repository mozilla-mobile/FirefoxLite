package org.mozilla.focus.utils;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;

public class DialogUtils {

    public static final int APP_CREATE_THRESHOLD_FOR_RATE_APP = 6;
    public static final int APP_CREATE_THRESHOLD_FOR_SHARE_APP = 11;

    public static void showRateAppDialog(final Context context) {
        if(context == null) {
            return;
        }
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_rate_app_dialog, null);
        dialogView.findViewById(R.id.dialog_rate_app_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialogView.findViewById(R.id.dialog_rate_app_btn_go_rate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String appPackageName = context.getPackageName();
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException ex) {
                    //No google play install
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialogView.findViewById(R.id.dialog_rate_app_btn_feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent urlIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(context.getString(R.string.rate_app_feedback_url)));
                urlIntent.setClassName(context, MainActivity.class.getName());
                urlIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(urlIntent);
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Settings.getInstance(context).setRateAppDialogDidShow();
    }
}
