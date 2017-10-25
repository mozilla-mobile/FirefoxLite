package org.mozilla.focus.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;

public class DialogUtils {

    public static final int APP_CREATE_THRESHOLD_FOR_RATE_APP = 6;
    public static final int APP_CREATE_THRESHOLD_FOR_SHARE_APP = 11;

    public static void showRateAppDialog(final Context context) {
        if(context == null) {
            return;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
            }
        });

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_rate_app_dialog, null);
        dialogView.findViewById(R.id.dialog_rate_app_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null) {
                    dialog.dismiss();
                    telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
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
                telemetryFeedback(context, TelemetryWrapper.Value.POSITIVE);
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
                telemetryFeedback(context, TelemetryWrapper.Value.NEGATIVE);
            }
        });
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Settings.getInstance(context).setRateAppDialogDidShow();
    }

    private static void telemetryFeedback(final Context context, String value) {
        if (context instanceof MainActivity) {
            TelemetryWrapper.feedbackClickEvent(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS);
        } else if (context instanceof SettingsActivity) {
            TelemetryWrapper.feedbackClickEvent(value, TelemetryWrapper.Extra_Value.SETTING);
        }
    }

    public static void showShareAppDialog(final Context context) {
        if(context == null) {
            return;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
            }
        });

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_share_app_dialog, null);
        dialogView.findViewById(R.id.dialog_share_app_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null) {
                    dialog.dismiss();
                    telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
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
                telemetryShareApp(context, TelemetryWrapper.Value.SHARE);
            }
        });
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Settings.getInstance(context).setShareAppDialogDidShow();
    }

    private static void telemetryShareApp(final Context context, String value) {
        if (context instanceof MainActivity) {
            TelemetryWrapper.promoteShareClickEvent(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS);
        } else if (context instanceof SettingsActivity) {
            TelemetryWrapper.promoteShareClickEvent(value, TelemetryWrapper.Extra_Value.SETTING);
        }
    }

    public static void showScreenshotOnBoardingDialog(final Context context) {
        if(context == null || !Settings.getInstance(context).shouldShowScreenshotOnBoarding()) {
            return;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context, R.style.TransparentAlertDialog).create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                telemetryScreenshotOnBoarding(TelemetryWrapper.Value.DISMISS);
            }
        });

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_screenshot_onboarding_dialog, null);

        dialogView.findViewById(R.id.dialog_screenshot_on_boarding_btn_got_it).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialog != null) {
                    dialog.dismiss();
                    telemetryScreenshotOnBoarding(TelemetryWrapper.Value.POSITIVE);
                }
            }
        });
        dialogView.findViewById(R.id.dialog_background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialog != null) {
                    dialog.dismiss();
                    telemetryScreenshotOnBoarding(TelemetryWrapper.Value.DISMISS);
                }
            }
        });
        dialog.setView(dialogView);
        dialog.show();
        Settings.getInstance(context).setScreenshotOnBoardingDone();
    }

    private static void telemetryScreenshotOnBoarding(String value) {
        TelemetryWrapper.promoteScreenShotClickEvent(value);
    }
}
