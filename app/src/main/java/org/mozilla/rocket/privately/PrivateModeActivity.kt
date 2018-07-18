/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import androidx.navigation.Navigation
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE

class PrivateModeActivity : LocaleAwareAppCompatActivity(), FragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_mode)

        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
    }

    @Override
    override fun onSupportNavigateUp() = getNavController().navigateUp()

    override fun applyLocale() {}

    override fun onNotified(from: Fragment, type: FragmentListener.TYPE, payload: Any?) {
        when (type) {
            TYPE.TOGGLE_PRIVATE_MODE -> pushToBack()
            TYPE.SHOW_URL_INPUT -> showUrlInput(payload)
            TYPE.DISMISS_URL_INPUT -> dismissUrlInput()
            TYPE.OPEN_URL_IN_CURRENT_TAB -> openUrl(payload)
            TYPE.OPEN_URL_IN_NEW_TAB -> openUrl(payload)
            else -> {
            }
        }
    }

    override fun onBackPressed() {
        val cnt = supportFragmentManager.backStackEntryCount
        if (cnt != 0) {
            supportFragmentManager.popBackStack()
        } else {
            val controller = getNavController()
            if (controller.currentDestination.id == R.id.fragment_private_home_screen) {
                super.onBackPressed()
            } else {
                getNavController().navigateUp()
            }
        }
    }

    private fun pushToBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        overridePendingTransition(0, R.anim.pb_exit)
    }

    private fun showUrlInput(payload: Any?) {
        val url = payload?.toString() ?: ""

        var frgMgr = supportFragmentManager

        if (isUrlInputDisplaying()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return
        }
        val urlFragment: UrlInputFragment = UrlInputFragment.create(url, null)
        val transaction = frgMgr.beginTransaction()
        transaction.add(R.id.private_nav_host_fragment, urlFragment, UrlInputFragment.FRAGMENT_TAG)
                .addToBackStack(UrlInputFragment.FRAGMENT_TAG)
                .commit()
    }

    private fun dismissUrlInput() {
        if (isUrlInputDisplaying()) {
            supportFragmentManager.popBackStack()
        }
    }

    private fun openUrl(payload: Any?) {
        val url = payload?.toString() ?: ""

        ViewModelProviders.of(this)
                .get(SharedViewModel::class.java)
                .setUrl(url)

        dismissUrlInput()
        val controller = getNavController()
        if (controller.currentDestination.id == R.id.fragment_private_home_screen) {
            controller.navigate(R.id.action_private_home_to_browser)
        }
    }

    private fun isUrlInputDisplaying(): Boolean {
        val frg = supportFragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG)
        return ((frg != null) && frg.isAdded && !frg.isRemoving)
    }

    private fun getNavController() = Navigation.findNavController(this, R.id.private_nav_host_fragment)
}
