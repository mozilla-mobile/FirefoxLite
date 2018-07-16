/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.rocket.privately.home.PrivateHomeFragment

class PrivateModeActivity : LocaleAwareAppCompatActivity(), FragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_mode)

        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility

        showHomeScreen()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        pushToBack()
    }

    override fun applyLocale() {}

    override fun onNotified(from: Fragment, type: FragmentListener.TYPE, payload: Any?) {
        when (type) {
            FragmentListener.TYPE.TOGGLE_PRIVATE_MODE -> pushToBack()
            else -> {
            }
        }
    }

    private fun showHomeScreen() {
        val fragment = this.createHomeFragment()
        this.supportFragmentManager.beginTransaction()
                .add(R.id.container, fragment, PrivateHomeFragment.FRAGMENT_TAG)
                .commit()
    }

    private fun createHomeFragment(): PrivateHomeFragment {
        return PrivateHomeFragment.create()
    }

    private fun pushToBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        overridePendingTransition(0, R.anim.pb_exit)
    }
}
