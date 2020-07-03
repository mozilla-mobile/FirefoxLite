/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.home.topsites.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity

class AddNewTopSitesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, AddNewTopSitesFragment())
            .commit()

        // Ensure all locale specific Strings are initialised on first run, we don't set the title
        // anywhere before now (the title can only be set via AndroidManifest, and ensuring
        // that that loads the correct locale string is tricky).
        applyLocale()
    }

    override fun applyLocale() {
        setTitle(R.string.label_menu_add_top_sites)
    }

    override fun getNightModeCover(): View? {
        return null
    }

    companion object {
        fun getStartIntent(context: Context): Intent {
            return Intent(context, AddNewTopSitesActivity::class.java)
        }
    }
}