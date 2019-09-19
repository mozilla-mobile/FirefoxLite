package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider

class ShoppingSearchActivity : BaseActivity(), TabsSessionProvider.SessionHost {

    private var sessionManager: SessionManager? = null
    private lateinit var tabViewProvider: TabViewProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_search)
        tabViewProvider = PrivateTabViewProvider(this)
        makeStatusBarTransparent()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager?.destroy()
    }

    override fun applyLocale() = Unit

    override fun getSessionManager(): SessionManager {
        if (sessionManager == null) {
            sessionManager = SessionManager(tabViewProvider)
        }

        // we just created it, it definitely not null
        return sessionManager!!
    }

    private fun makeStatusBarTransparent() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, ShoppingSearchActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
