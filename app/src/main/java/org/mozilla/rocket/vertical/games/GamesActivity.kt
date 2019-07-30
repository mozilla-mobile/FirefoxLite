package org.mozilla.rocket.vertical.games

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_games.games_tabs
import kotlinx.android.synthetic.main.activity_games.view_pager
import org.mozilla.focus.R

class GamesActivity : FragmentActivity() {

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: GamesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gamesViewModel = ViewModelProviders.of(this, GamesViewModelFactory.INSTANCE).get(GamesViewModel::class.java)
        setContentView(R.layout.activity_games)
        initViewPager()
        initTabLayout()
        observeGameAction()
    }

    private fun initViewPager() {
        adapter = GamesAdapter(supportFragmentManager, this)
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

@SuppressLint("WrongConstant")
private class GamesAdapter(fm: FragmentManager, activity: FragmentActivity) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> BrowserGamesFragment()
            1 -> GamesActivity.PremiumGamesFragment()
            else -> throw IndexOutOfBoundsException("position: $position")
        }
    }

    override fun getCount(): Int = GAMES_TAB_COUNT

    override fun getPageTitle(position: Int): CharSequence? {
        val resId = when (position) {
            0 -> R.string.games_tab_title_browser_games
            1 -> R.string.games_tab_title_premium_games
            else -> throw IndexOutOfBoundsException("position: $position")
        }

        return resource.getString(resId)
    }

    companion object {
        const val GAMES_TAB_COUNT = 2
    }
}