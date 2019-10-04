package org.mozilla.rocket.content.games.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_games.*
import org.mozilla.focus.R
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.Constants
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.games.ui.adapter.GameTabsAdapter
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

class GamesActivity : FragmentActivity() {

    @Inject
    lateinit var gamesViewModelCreator: Lazy<GamesViewModel>

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: GameTabsAdapter
    private lateinit var uiMessageReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        gamesViewModel = getViewModel(gamesViewModelCreator)
        setContentView(R.layout.activity_games)
        initViewPager()
        initTabLayout()
        initToolBar()

        initBroadcastReceivers()
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

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            when (games_tabs.selectedTabPosition) {
                TAB_INSTANT -> gamesViewModel.getLatestInstantGames()
                TAB_DOWNLOAD -> gamesViewModel.getLatestDownloadGames()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val uiActionFilter = IntentFilter()
        uiActionFilter.addCategory(Constants.CATEGORY_FILE_OPERATION)
        uiActionFilter.addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH)
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver)
    }

    private fun initBroadcastReceivers() {
        uiMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.ACTION_NOTIFY_RELOCATE_FINISH) {
                    DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), snack_bar_container, this@GamesActivity.javaClass.name)
                }
            }
        }
    }

    companion object {
        const val TAB_INSTANT = 0
        const val TAB_DOWNLOAD = 1
        fun getStartIntent(context: Context) =
            Intent(context, GamesActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
