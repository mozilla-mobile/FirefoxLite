/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.mozilla.focus.FocusApplication
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.rocket.component.LaunchIntentDispatcher

class ShortcutUtils {
    companion object {
        private const val SHORTCUT_ID = "pb_shortcut"

        fun createShortcut(context: Context) {
            val shortcutIntent = Intent(Intent.ACTION_MAIN)
            shortcutIntent.setClassName(context, AppConstants.LAUNCHER_PRIVATE_ACTIVITY_ALIAS)
            shortcutIntent.putExtra(
                    LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_PRIVATE_MODE_SHORTCUT.value,
                    true
            )
            shortcutIntent.flags = shortcutIntent.flags or
                    Intent.FLAG_ACTIVITY_NEW_TASK

            val icon = IconCompat.createWithResource(context, R.drawable.ic_pb_launcher)
            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                    .setShortLabel(context.getString(
                            R.string.private_browsing_shortcut_name,
                            context.getString(R.string.app_name)
                    ))
                    .setIntent(shortcutIntent)
                    .setIcon(icon)
                    .build()

            // Known issue: when there is already an existing shortcut, intendSender will be called
            // immediately when system shortcut dialog is shown. i.e. Home will be brought up, and then
            // show the system shortcut dialog. UX currently agreed with this behavior since it's
            // not often the user will add shortcut again.
            val intentSender = IntentUtils.getLauncherHomePendingIntent(context).intentSender

            (context.applicationContext as FocusApplication)
                    .settings
                    .privateBrowsingSettings
                    .setPrivateShortcutCreated()

            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                TelemetryWrapper.createPrivateShortcut()
                ShortcutManagerCompat.requestPinShortcut(context, shortcut, intentSender)
            }
        }
    }
}
