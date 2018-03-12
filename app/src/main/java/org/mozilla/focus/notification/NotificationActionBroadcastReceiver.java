/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.IntentUtils;

public class NotificationActionBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG = "NotifyActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (IntentUtils.ACTION_RATE_STAR.equals(intent.getAction())) {

            IntentUtils.goToPlayStore(context);

        } else if (IntentUtils.ACTION_FEEDBACK.equals(intent.getAction())) {

            final Intent openFeedbackPage = IntentUtils.createInternalOpenUrlIntent(context,
                    context.getString(R.string.rate_app_feedback_url), true);
            context.startActivity(openFeedbackPage);
        } else {
            Log.e(TAG, "Not a valid action");
        }

        NotificationManagerCompat.from(context).cancel(NotificationId.LOVE_ROCKET);
    }

}
