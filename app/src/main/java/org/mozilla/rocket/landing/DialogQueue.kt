/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.landing

import java.util.ArrayDeque

class DialogQueue {
    private var currentDialog: DialogDelegate? = null
    private val queue = ArrayDeque<DialogDelegate>()

    /**
     * FIFO queue for dialogs to be shown at the same time
     * @param dialog operations dialog needs to support in order to work with DialogQueue
     */
    fun enqueue(dialog: DialogDelegate) {
        if (isShowing()) {
            queue.offer(dialog)
        } else {
            showDialog(dialog)
        }
    }

    /**
     * Show dialog only if there's no any other dialog showing
     * @param dialog operations dialog needs to support in order to work with DialogQueue
     */
    fun tryShow(dialog: DialogDelegate): Boolean {
        if (!isShowing()) {
            showDialog(dialog)
            return true
        }
        return false
    }

    private fun nextDialog() {
        if (queue.isEmpty()) {
            return
        }
        showDialog(queue.removeFirst())
    }

    private fun showDialog(dialog: DialogDelegate) {
        currentDialog = dialog
        dialog.setOnDismissListener {
            currentDialog = null
            nextDialog()
        }
        dialog.show()
    }

    private fun isShowing() = currentDialog != null

    interface DialogDelegate {
        fun setOnDismissListener(listener: () -> Unit)
        fun show()
    }
}
