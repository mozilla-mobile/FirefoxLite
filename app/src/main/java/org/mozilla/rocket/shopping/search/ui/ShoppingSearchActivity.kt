package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider

class ShoppingSearchActivity : BaseActivity(), TabsSessionProvider.SessionHost {

    private var sessionManager: SessionManager? = null
    private lateinit var tabViewProvider: PrivateTabViewProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_search)
        tabViewProvider = PrivateTabViewProvider(this)
    }

    override fun applyLocale() {}

    override fun getSessionManager(): SessionManager {
        if (sessionManager == null) {
            sessionManager = SessionManager(tabViewProvider)
        }

        // we just created it, it definitely not null
        return sessionManager!!
    }

    companion object {
        fun getStartIntent(context: Context) = Intent(context, ShoppingSearchActivity::class.java)
    }
}