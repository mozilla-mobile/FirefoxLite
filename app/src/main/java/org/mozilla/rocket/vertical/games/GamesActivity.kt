package org.mozilla.rocket.vertical.games

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_games.games_tabs
import kotlinx.android.synthetic.main.activity_games.view_pager
import org.mozilla.focus.R
import org.mozilla.rocket.vertical.games.adapter.GameTabsAdapter

class GamesActivity : FragmentActivity() {

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: GameTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gamesViewModel = ViewModelProviders.of(this, GamesViewModelFactory.INSTANCE).get(GamesViewModel::class.java)
        setContentView(R.layout.activity_games)
        initViewPager()
        initTabLayout()
        observeGameAction()
    }

    private fun initViewPager() {
        adapter = GameTabsAdapter(supportFragmentManager, this)
        view_pager.apply {
            adapter = this@GamesActivity.adapter
        }
    }

    private fun initTabLayout() {
        games_tabs.setupWithViewPager(view_pager)
    }

    private fun observeGameAction() {
        gamesViewModel.showToast.observe(this, Observer { message ->
            Toast.makeText(applicationContext, getString(message.stringResId, *message.args), message.duration).show()
        })
    }

    class PremiumGamesFragment : Fragment()

    companion object {
        fun getStartIntent(context: Context) = Intent(context, GamesActivity::class.java)
    }
}