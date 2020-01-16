package org.mozilla.rocket.content.game.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_game.*
import org.mozilla.focus.R
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Constants
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.game.ui.adapter.GameTabsAdapter
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.extension.isLaunchedFromHistory
import org.mozilla.rocket.util.sha256
import javax.inject.Inject

class GameActivity : FragmentActivity() {

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var telemetryViewModel: VerticalTelemetryViewModel

    private lateinit var adapter: GameTabsAdapter
    private lateinit var uiMessageReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)
        setContentView(R.layout.activity_game)
        initViewPager()
        initTabLayout()
        initToolBar()

        initBroadcastReceivers()

        intent.extras?.let {
            if (!isLaunchedFromHistory()) {
                parseDeepLink(it)
            }
        }
    }

    private fun parseDeepLink(bundle: Bundle): Boolean {
        when (bundle.getString(EXTRA_DEEP_LINK) ?: return false) {
            DEEP_LINK_GAME_ITEM_PAGE -> openGameItemPageFromDeepLink(bundle.getParcelable(EXTRA_GAME_ITEM_DATA)!!)
        }

        return true
    }

    private fun openGameItemPageFromDeepLink(gameItemData: DeepLink.GameItemPage.Data) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.GAME,
            gameItemData.feed,
            gameItemData.source,
            "", // no need when triggered by deep link
            gameItemData.url.sha256(),
            "", // no need when triggered by deep link
            System.currentTimeMillis()
        )
        startActivity(GameModeActivity.getStartIntent(this, gameItemData.url, telemetryData))
    }

    private fun initViewPager() {
        adapter = GameTabsAdapter(supportFragmentManager, this)
        view_pager.apply {
            adapter = this@GameActivity.adapter
        }

        Looper.myQueue().addIdleHandler {
            telemetryViewModel.onCategorySelected(TelemetryWrapper.Extra_Value.INSTANT_GAME)
            false
        }
    }

    private fun initTabLayout() {
        games_tabs.setupWithViewPager(view_pager)
        games_tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) = Unit

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = when (tab.position) {
                    GameTabsAdapter.TabItem.TYPE_INSTANT_GAME_TAB -> TelemetryWrapper.Extra_Value.INSTANT_GAME
                    else -> TelemetryWrapper.Extra_Value.DOWNLOAD_GAME
                }

                telemetryViewModel.onCategorySelected(category)
            }
        })
        if (adapter.count > 1) {
            games_tabs.visibility = View.VISIBLE
        } else {
            games_tabs.visibility = View.GONE
        }
    }

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            initViewPager()
            telemetryViewModel.onRefreshClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        val uiActionFilter = IntentFilter()
        uiActionFilter.addCategory(Constants.CATEGORY_FILE_OPERATION)
        uiActionFilter.addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH)
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter)
        telemetryViewModel.onSessionStarted(TelemetryWrapper.Extra_Value.GAME)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver)
        telemetryViewModel.onSessionEnded()
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

    sealed class DeepLink(val name: String) {
        data class GameItemPage(val data: Data) : DeepLink(DEEP_LINK_GAME_ITEM_PAGE) {
            @Parcelize
            data class Data(val url: String, val feed: String, val source: String) : Parcelable
        }
    }

    companion object {
        private const val EXTRA_DEEP_LINK = "extra_deep_link"
        private const val EXTRA_GAME_ITEM_DATA = "extra_game_item_data"
        private const val DEEP_LINK_GAME_ITEM_PAGE = "deep_link_game_item_page"

        fun getStartIntent(context: Context) =
            Intent(context, GameActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        fun getStartIntent(context: Context, deepLink: DeepLink): Intent = getStartIntent(context).apply {
            putExtras(Bundle().apply {
                putString(EXTRA_DEEP_LINK, deepLink.name)
                when (deepLink) {
                    is DeepLink.GameItemPage -> {
                        putParcelable(EXTRA_GAME_ITEM_DATA, deepLink.data)
                    }
                }
            })
        }
    }
}
