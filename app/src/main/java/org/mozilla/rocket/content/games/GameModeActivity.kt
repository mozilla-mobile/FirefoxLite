package org.mozilla.rocket.content.games

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.utils.SafeIntent

class GameModeActivity : BaseActivity() {

    private lateinit var webview: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_mode)

        webview = findViewById(R.id.webview_game)
        webview.settings.javaScriptEnabled = true
        webview.settings.useWideViewPort = true
        webview.settings.loadWithOverviewMode = true

        handleIntent(intent)
    }

    override fun applyLocale() = Unit

    private fun handleIntent(intent: Intent?) {
        val safeIntent = intent?.let { SafeIntent(it) } ?: return
        webview.loadUrl(safeIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    companion object {
        fun create(context: Context, linkUrl: String): Intent {
            val intent = Intent(context, GameModeActivity::class.java)
            intent.putExtra(Intent.EXTRA_TEXT, linkUrl)
            return intent
        }
    }
}