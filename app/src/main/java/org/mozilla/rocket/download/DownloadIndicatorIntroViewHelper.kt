package org.mozilla.rocket.download

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import org.mozilla.focus.R

object DownloadIndicatorIntroViewHelper {

    fun initDownloadIndicatorIntroView(fragment: Fragment, targetView: View?, parentView: ViewGroup?) {
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
                val menu = view.findViewById<ImageView>(R.id.download_indicator_intro_menu)
                // Adjust menu position
                val menuParams = menu.layoutParams as ConstraintLayout.LayoutParams
                menuParams.setMargins(0, 0, metrics.widthPixels - menuParams.width - location[0], metrics.heightPixels - menuParams.height - location[1])

                // Adjust pointer position
                val pointer = view.findViewById<ImageView>(R.id.download_indicator_intro_pointer)
                val pointParams = pointer.layoutParams as ConstraintLayout.LayoutParams
                pointParams.setMargins(0, 0, metrics.widthPixels - menuParams.width / 2 - pointParams.width / 2 - location[0], 0)

                rootView?.setOnClickListener { _ -> rootView.visibility = View.GONE }
                menu.setOnClickListener { _ ->
                    targetView.performClick()
                    rootView?.visibility = View.GONE
                }
                menu.setOnLongClickListener { _ ->
                    targetView.performLongClick()
                    rootView?.visibility = View.GONE
                    false
                }
            }
            // Align the duration to wait for downloading toast(Toast.LENGTH_LONG) is dismissed
        }, 3500)
    }
}
