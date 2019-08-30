package org.mozilla.rocket.content.news

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
        fun getStartIntent(context: Context) = Intent(context, NewsActivity::class.java)
    }
}
