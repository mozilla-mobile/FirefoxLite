package org.mozilla.rocket.content.news.ui.v2

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
}
