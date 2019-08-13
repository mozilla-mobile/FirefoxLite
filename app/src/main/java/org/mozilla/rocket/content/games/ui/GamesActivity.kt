package org.mozilla.rocket.content.games.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_games.games_tabs
import kotlinx.android.synthetic.main.activity_games.view_pager
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.games.ui.adapter.GameTabsAdapter
import org.mozilla.rocket.content.viewModelProvider
import javax.inject.Inject

class GamesActivity : FragmentActivity() {

    @Inject
    lateinit var gamesViewModelFactory: GamesViewModelFactory

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: GameTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        gamesViewModel = viewModelProvider(gamesViewModelFactory)
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
            Toast.makeText(applicationContext, getString(message.stringResId, message.args[0]), message.duration).show()
            // TODO: testing code, needs to be removed
            startActivity(GameModeActivity.create(this, message.args[1]))
        })
    }

    class PremiumGamesFragment : Fragment()

    companion object {
        fun getStartIntent(context: Context) = Intent(context, GamesActivity::class.java)
    }
}