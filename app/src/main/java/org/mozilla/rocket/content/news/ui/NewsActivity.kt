package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.focus.R

class NewsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.news_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NewsTabFragment.newInstance())
                .commitNow()
        }
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, NewsActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
