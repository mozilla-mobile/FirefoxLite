package org.mozilla.rocket.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

fun Context.toActivity(): Activity = when {
    this is Activity -> this
    this is ContextWrapper -> this.baseContext.toActivity()
    else -> error("context is not a Activity")
}

fun Context.toFragmentActivity(): FragmentActivity =
        this.toActivity().let {
            if (it is FragmentActivity) {
                it
            } else {
                error("context is not a FragmentActivity")
            }
        }