/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import androidx.fragment.app.Fragment
import org.mozilla.focus.navigation.ScreenNavigator.Screen

class FirstrunFragment : Fragment(), Screen {

    override fun getFragment(): Fragment {
        return this
    }

    // TODO: Evan, add back to version 2.5 onboarding
//    private fun wrapButtonClickListener(onClickListener: View.OnClickListener): View.OnClickListener {
//        return View.OnClickListener { view ->
//            if (view.id == R.id.finish) {
//                activity?.sendBroadcast(Intent(activity, PeriodicReceiver::class.java).apply {
//                    action = FirstLaunchWorker.ACTION
//                })
//            }
//            onClickListener.onClick(view)
//        }
//    }
//
//    private fun finishFirstrun() {
//        NewFeatureNotice.getInstance(context).setFirstRunDidShow()
//        NewFeatureNotice.getInstance(context).setLiteUpdateDidShow()
//        (activity as MainActivity).firstrunFinished()
//    }

    companion object {
        @JvmStatic
        fun create(): FirstrunFragment {
            return FirstrunFragment()
        }
    }
}
