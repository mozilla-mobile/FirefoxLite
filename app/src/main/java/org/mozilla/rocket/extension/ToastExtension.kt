package org.mozilla.rocket.extension

import android.content.Context
import android.widget.Toast
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.widget.FxToast

fun Context.showFxToast(toastMessage: ToastMessage) {
    FxToast.show(this, toastMessage.message ?: getString(toastMessage.stringResId!!, toastMessage.args), toastMessage.duration)
}

fun Context.showToast(toastMessage: ToastMessage) {
    Toast.makeText(this, toastMessage.message ?: getString(toastMessage.stringResId!!, toastMessage.args), toastMessage.duration).show()
}