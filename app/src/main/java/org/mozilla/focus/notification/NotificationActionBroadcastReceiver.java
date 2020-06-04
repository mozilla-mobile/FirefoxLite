/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import org.mozilla.focus.FocusApplication;
import org.mozilla.focus.R;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;

import static org.mozilla.focus.notification.RocketMessagingService.STR_PUSH_COMMAND;
import static org.mozilla.focus.notification.RocketMessagingService.STR_PUSH_DEEP_LINK;
import static org.mozilla.focus.notification.RocketMessagingService.STR_PUSH_OPEN_URL;

// This class handles all click/actions users performed on a notification.
// This ensures that all telemetry works for action/click are in one place.
// The UI code will be responsible for telemetry work for displaying the notifications.
public class NotificationActionBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG = "NotifyActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        final String action = intent.getAction();
        final Bundle bundle = intent.getExtras();
        if (bundle == null || action == null || !IntentUtils.ACTION_NOTIFICATION.equals(action)) {
            return;
        }
        Intent nexStep = null;
        boolean isAppInBackground = !((FocusApplication) context.getApplicationContext()).isForeground();

        if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_DELETE_NOTIFICATION)) {
            String source = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_NOTIFICATION_SOURCE);
            String link = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_LINK, "");
            if (IntentUtils.NOTIFICATION_SOURCE_FIREBASE.equals(source)) {
                String messageId = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_MESSAGE_ID, "");
                TelemetryWrapper.dismissNotification(link, messageId);
            } else if (IntentUtils.NOTIFICATION_SOURCE_FIRSTRUN.equals(source)) {
                String messageId = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_MESSAGE_ID, "");
                TelemetryWrapper.dismissFirstrunNotification(link, messageId, isAppInBackground);
            }

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_NOTIFICATION)) {
            nexStep = new Intent();
            nexStep.setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
            nexStep.putExtra(STR_PUSH_OPEN_URL, intent.getStringExtra(IntentUtils.EXTRA_NOTIFICATION_OPEN_URL));
            nexStep.putExtra(STR_PUSH_COMMAND, intent.getStringExtra(IntentUtils.EXTRA_NOTIFICATION_COMMAND));
            nexStep.putExtra(STR_PUSH_DEEP_LINK, intent.getStringExtra(IntentUtils.EXTRA_NOTIFICATION_DEEP_LINK));

            String source = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_NOTIFICATION_SOURCE);
            String link = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_LINK, "");
            if (IntentUtils.NOTIFICATION_SOURCE_FIREBASE.equals(source)) {
                String messageId = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_MESSAGE_ID, "");
                TelemetryWrapper.openNotification(link, messageId, false);
            } else if (IntentUtils.NOTIFICATION_SOURCE_FIRSTRUN.equals(source)) {
                String messageId = bundle.getString(IntentUtils.EXTRA_NOTIFICATION_MESSAGE_ID, "");
                TelemetryWrapper.openD1Notification(link, messageId, isAppInBackground);
            }

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_ACTION_RATE_STAR)) {

            IntentUtils.goToPlayStore(context);

            TelemetryWrapper.clickRateApp(TelemetryWrapper.Value.POSITIVE, TelemetryWrapper.Extra_Value.NOTIFICATION);

            NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_FIREFOX);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_ACTION_FEEDBACK)) {

            nexStep = IntentUtils.createInternalOpenUrlIntent(context,
                    context.getString(R.string.rate_app_feedback_url), true);

            // Users set negative feedback, don't ask them to rate/feedback again.
            Settings.getInstance(context).setShareAppDialogDidShow();

            TelemetryWrapper.clickRateApp(TelemetryWrapper.Value.NEGATIVE, TelemetryWrapper.Extra_Value.NOTIFICATION);

            NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_FIREFOX);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_DEFAULT_BROWSER)) {
            nexStep = IntentUtils.createSetDefaultBrowserIntent(context);

            TelemetryWrapper.clickDefaultSettingNotification();

            NotificationManagerCompat.from(context).cancel(NotificationId.DEFAULT_BROWSER);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_LOVE_FIREFOX)) {
            nexStep = new Intent();
            nexStep.setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
            nexStep.putExtra(IntentUtils.EXTRA_SHOW_RATE_DIALOG, true);

            TelemetryWrapper.clickRateApp(null, TelemetryWrapper.Extra_Value.NOTIFICATION);

            NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_FIREFOX);

        } else if (bundle.getBoolean(IntentUtils.EXTRA_NOTIFICATION_CLICK_PRIVACY_POLICY_UPDATE)) {
            nexStep = IntentUtils.createInternalOpenUrlIntent(context, SupportUtils.getPrivacyURL(), true);

            // TODO: telemetry

            NotificationManagerCompat.from(context).cancel(NotificationId.PRIVACY_POLICY_UPDATE);

        } else {
            Log.e(TAG, "Not a valid action");
        }

        bundle.clear();
        if (nexStep != null) {
            nexStep.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(nexStep);
        }
    }

}
