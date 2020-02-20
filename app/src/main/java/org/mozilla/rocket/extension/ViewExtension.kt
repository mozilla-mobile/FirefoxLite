package org.mozilla.rocket.extension

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

fun Context.dpToPx(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

fun View.dpToPx(value: Float): Int = context.dpToPx(value)

fun Fragment.dpToPx(value: Float): Int = requireContext().dpToPx(value)

fun Activity.inflate(@LayoutRes layoutRes: Int, root: ViewGroup? = null): View =
        LayoutInflater.from(this).inflate(layoutRes, root)

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)