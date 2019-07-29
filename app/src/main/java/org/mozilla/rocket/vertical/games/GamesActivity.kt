package org.mozilla.rocket.vertical.games

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.mozilla.focus.R

class GamesActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
    }

    companion object {
        fun getStartIntent(context: Context) = Intent(context, GamesActivity::class.java)
    }
}