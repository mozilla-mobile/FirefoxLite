package org.mozilla.rocket.content.games.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import kotlinx.android.synthetic.main.toolbar_game.refresh_button
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
    private lateinit var snackBarContainer: View
    private val LOG_TAG = "GameActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        gamesViewModel = getViewModel(gamesViewModelCreator)
        setContentView(R.layout.activity_games)
        initViewPager()
        initTabLayout()
        initToolBar()

        snackBarContainer = findViewById(R.id.container)
        initBroadcastReceivers()
    }

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            gamesViewModel.onRefreshGameListButtonClicked()
        }
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
                    DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), snackBarContainer, LOG_TAG)
                }
            }
        }
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, GamesActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
