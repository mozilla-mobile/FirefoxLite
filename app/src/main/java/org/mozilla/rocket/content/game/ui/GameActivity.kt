package org.mozilla.rocket.content.game.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_game.*
import org.mozilla.focus.R
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.Constants
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.game.ui.adapter.GameTabsAdapter

class GameActivity : FragmentActivity() {

    private lateinit var adapter: GameTabsAdapter
    private lateinit var uiMessageReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        initViewPager()
        initTabLayout()
        initToolBar()

        initBroadcastReceivers()
    }

    private fun initViewPager() {
        adapter = GameTabsAdapter(supportFragmentManager, this)
        view_pager.apply {
            adapter = this@GameActivity.adapter
        }
    }

    private fun initTabLayout() {
        games_tabs.setupWithViewPager(view_pager)
    }

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            initViewPager()
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
                    DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), snack_bar_container, this@GameActivity.javaClass.name)
                }
            }
        }
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, GameActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
