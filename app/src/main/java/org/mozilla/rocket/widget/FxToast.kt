package org.mozilla.rocket.widget

import android.app.Service
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.fx_toast.view.text
import org.mozilla.focus.R
import java.lang.ref.WeakReference

object FxToast {
    private var toastWeak: WeakReference<Toast?> = WeakReference(null)
    private var inflater: LayoutInflater? = null

    fun show(appContext: Context, msg: String, duration: Int = Toast.LENGTH_LONG) {
        val lastToast = toastWeak.get()
        val newToast = if (lastToast != null) {
            createToast(appContext, duration, customView = lastToast.view.apply { text.text = msg })
        } else {
            createToast(appContext, duration, customView = inflateCustomView(appContext, msg))
        }
        toastWeak = WeakReference(newToast)
        lastToast?.cancel()
        newToast.show()
    }

    private fun inflateCustomView(appContext: Context, msg: String): View =
            getLayoutInflater(appContext).inflate(R.layout.fx_toast, null).apply {
                text.text = msg
            }

    private fun createToast(appContext: Context, duration: Int, customView: View) = Toast(appContext)
            .apply {
                setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 0)
                view = customView
                this.duration = duration
            }

    private fun getLayoutInflater(appContext: Context): LayoutInflater {
        if (inflater == null) {
            inflater = appContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        return requireNotNull(inflater)
    }
}
