package org.mozilla.focus.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
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
                Settings.getInstance(context).setShareAppDialogDidShow();
                IntentUtils.openUrl(context, context.getString(R.string.rate_app_feedback_url));
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

    public static void showShareAppDialog(final Context context) {
        if(context == null) {
            return;
        }
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_share_app_dialog, null);
        dialogView.findViewById(R.id.dialog_share_app_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialogView.findViewById(R.id.dialog_share_app_btn_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
                sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_promotion_text));
                context.startActivity(Intent.createChooser(sendIntent, null));
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Settings.getInstance(context).setShareAppDialogDidShow();
    }

    public static void showScreenshotOnBoardingDialog(Context context) {
        if(context == null || !Settings.getInstance(context).shouldShowScreenshotOnBoarding()) {
            return;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context, R.style.TransparentAlertDialog).create();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_screenshot_onboarding_dialog, null);

        View.OnClickListener dismiss = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null)
                    dialog.dismiss();
            }
        };

        dialogView.findViewById(R.id.dialog_screenshot_on_boarding_btn_got_it).setOnClickListener(dismiss);
        dialogView.findViewById(R.id.dialog_background).setOnClickListener(dismiss);
        dialog.setView(dialogView);
        dialog.show();
        Settings.getInstance(context).setScreenshotOnBoardingDone();
    }
}
