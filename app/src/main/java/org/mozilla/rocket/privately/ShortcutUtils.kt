/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.content.Context
import android.content.Intent
import android.support.v4.content.pm.ShortcutInfoCompat
import android.support.v4.content.pm.ShortcutManagerCompat
import android.support.v4.graphics.drawable.IconCompat
import org.mozilla.focus.R
import org.mozilla.focus.utils.AppConstants
import org.mozilla.rocket.component.LaunchIntentDispatcher

class ShortcutUtils {
    companion object {
        private const val SHORTCUT_ID = "pb_shortcut"

        fun createShortcut(context: Context) {
            val shortcutIntent = Intent(Intent.ACTION_VIEW)
            shortcutIntent.setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS)
            shortcutIntent.putExtra(
                    LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_PRIVATE_MODE.value,
                    true
            )
            shortcutIntent.flags = shortcutIntent.flags or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            val icon = IconCompat.createWithResource(context, R.drawable.ic_pb_launcher)
            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                    .setShortLabel(context.getString(
                            R.string.private_browsing_shortcut_name,
                            context.getString(R.string.app_name)
                    ))
                    .setIntent(shortcutIntent)
                    .setIcon(icon)
                    .build()

            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            }
        }
    }
}
