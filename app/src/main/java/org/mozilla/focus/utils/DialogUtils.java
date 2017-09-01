package org.mozilla.focus.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

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

    public static void showShareAppDialog(final Context context) {
        if(context == null) {
            return;
        }
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_share_app_dialog, null);
        dialogView.findViewById(R.id.dialog_rate_app_btn_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String appPackageName = context.getPackageName();
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + appPackageName);
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

    public static AlertDialog showScreenshotOnBoardingDialog(Context context) {
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_screenshot_onboarding_dialog, null);
        
        TextView textView = (TextView) dialogView.findViewById(R.id.dialog_screenshot_on_boarding_text);
        ImageSpan imageSpan = new ImageSpan(context, R.drawable.action_capture);
        String emptyPrefix = context.getString(R.string.screenshot_on_boarding_text_msg_prefix);
        String emptyPostfix = context.getString(R.string.screenshot_on_boarding_text_msg_postfix);
        SpannableString spannableString = new SpannableString(emptyPrefix + emptyPostfix);

        int start = emptyPrefix.length();
        int end = start + 1;
        spannableString.setSpan(imageSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        textView.setText(spannableString);

        dialogView.findViewById(R.id.dialog_screenshot_on_boarding_btn_got_it).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null)
                    dialog.dismiss();
            }
        });
        dialog.setView(dialogView);
        dialog.show();
        Settings.getInstance(context).setScreenshotOnBoardingDone();
        return dialog;
    }
}
