package org.mozilla.rocket.download

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import org.mozilla.focus.R

object DownloadIndicatorIntroViewHelper {

    private const val TOAST_DELAY_LONG = 3500L

    interface OnViewInflated {
        fun onInflated(view: View)
    }

    fun initDownloadIndicatorIntroView(
        fragment: Fragment,
        targetView: View?,
        parentView: ViewGroup?,
        listener: OnViewInflated
    ) {
        targetView?.postDelayed({
            if (fragment.isResumed && parentView != null) {

                val location = IntArray(2)
                targetView.getLocationOnScreen(location)

                val wm = fragment.activity!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = wm.defaultDisplay
                val metrics = DisplayMetrics()
                display.getMetrics(metrics)

                val view = LayoutInflater.from(fragment.activity).inflate(R.layout.download_indicator_intro, parentView)
                val rootView = view.findViewById<View>(R.id.download_indicator_intro_root)
                listener.onInflated(rootView)

                val menu = view.findViewById<ImageView>(R.id.download_indicator_intro_menu)
                // Adjust menu position
                val menuParams = menu.layoutParams as ConstraintLayout.LayoutParams
                menuParams.setMargins(0, 0, metrics.widthPixels - menuParams.width - location[0], metrics.heightPixels - menuParams.height - location[1])

                // Adjust pointer position
                val pointer = view.findViewById<ImageView>(R.id.download_indicator_intro_pointer)
                val pointParams = pointer.layoutParams as ConstraintLayout.LayoutParams
                pointParams.setMargins(0, 0, metrics.widthPixels - menuParams.width / 2 - pointParams.width / 2 - location[0], 0)

                rootView?.setOnClickListener { rootView.visibility = View.GONE }
                menu.setOnClickListener {
                    targetView.performClick()
                    rootView?.visibility = View.GONE
                }
                menu.setOnLongClickListener {
                    targetView.performLongClick()
                    rootView?.visibility = View.GONE
                    false
                }
            }
            // TODO Confirm with UX to find a better way to do this
            // Align the duration to wait for downloading toast(Toast.LENGTH_LONG) is dismissed
        }, TOAST_DELAY_LONG)
    }
}
