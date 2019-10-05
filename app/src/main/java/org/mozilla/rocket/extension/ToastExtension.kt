package org.mozilla.rocket.extension

import android.content.Context
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.widget.FxToast

fun Context.showToast(toastMessage: ToastMessage) {
    FxToast.show(this, toastMessage.message ?: getString(toastMessage.stringResId!!, toastMessage.args), toastMessage.duration)
}