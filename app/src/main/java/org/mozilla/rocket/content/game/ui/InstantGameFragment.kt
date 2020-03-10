package org.mozilla.rocket.content.game.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_game.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.ShortcutUtils
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.component.LaunchIntentDispatcher
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.game.ui.adapter.InstantGameCategoryAdapterDelegate
import org.mozilla.rocket.content.game.ui.model.GameCategory
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class InstantGameFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var instantGameViewModelCreator: Lazy<InstantGameViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var instantGamesViewModel: InstantGameViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var adapter: DelegateAdapter
    private lateinit var recentPlayedSpotlightDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        instantGamesViewModel = getActivityViewModel(instantGameViewModelCreator)
        telemetryViewModel = getActivityViewModel(telemetryViewModelCreator)
        instantGamesViewModel.requestGameList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
        bindPageState()
        observeGameAction()
        observeRecentPlayedSpotlight()
        initNoResultView()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel, TelemetryWrapper.Extra_Value.INSTANT_GAME, telemetryViewModel))
                add(GameCategory::class, R.layout.item_game_category, InstantGameCategoryAdapterDelegate(instantGamesViewModel, telemetryViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@InstantGameFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        instantGamesViewModel.instantGameItems.observe(viewLifecycleOwner, Observer {
            adapter.setData(it)
            instantGamesViewModel.checkRecentPlayedSpotlight()
            telemetryViewModel.updateVersionId(TelemetryWrapper.Extra_Value.INSTANT_GAME, instantGamesViewModel.versionId)
        })
    }

    private fun bindPageState() {
        instantGamesViewModel.isDataLoading.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is InstantGameViewModel.State.Idle -> showContentView()
                is InstantGameViewModel.State.Loading -> showLoadingView()
                is InstantGameViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeGameAction() {
        runwayViewModel.openRunway.observe(viewLifecycleOwner, Observer { action ->
            context?.let {
                when (action.type) {
                    RunwayItem.TYPE_CONTENT_TAB -> {
                        startActivity(ContentTabActivity.getStartIntent(
                            it,
                            action.url,
                            action.telemetryData.copy(vertical = TelemetryWrapper.Extra_Value.GAME, versionId = instantGamesViewModel.versionId)))
                    }
                    RunwayItem.TYPE_EXTERNAL_LINK -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    else -> {
                        startActivity(GameModeActivity.getStartIntent(
                            it,
                            action.url,
                            action.telemetryData.copy(vertical = TelemetryWrapper.Extra_Value.GAME, versionId = instantGamesViewModel.versionId)))
                    }
                }
            }
        })

        instantGamesViewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is InstantGameViewModel.GameAction.Play -> {
                    val play: InstantGameViewModel.GameAction.Play = event
                    context?.let {
                        startActivity(GameModeActivity.getStartIntent(it, play.url, play.telemetryData))
                    }
                }
                is InstantGameViewModel.GameAction.Share -> {
                    val share: InstantGameViewModel.GameAction.Share = event
                    context?.let {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.gaming_vertical_share_message), share.url))
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(sendIntent, null))
                    }
                }
                is InstantGameViewModel.GameAction.CreateShortcut -> {
                    val shortcut: InstantGameViewModel.GameAction.CreateShortcut = event
                    context?.let {
                        createShortcut(it, shortcut.name, shortcut.url, shortcut.bitmap)
                    }
                }
            }
        })
    }

    private fun observeRecentPlayedSpotlight() {
        instantGamesViewModel.showRecentPlayedSpotlight.observe(viewLifecycleOwner, Observer {
            showRecentPlayedSpotlight()
        })
        instantGamesViewModel.dismissRecentPlayedSpotlight.observe(viewLifecycleOwner, Observer {
            closeRecentPlayedSpotlight()
        })
    }

    private fun initNoResultView() {
        no_result_view.setButtonOnClickListener(View.OnClickListener {
            instantGamesViewModel.onRetryButtonClicked()
        })
    }

    private fun createShortcut(context: Context, gameName: String, gameURL: String, gameIcon: Bitmap) {
        val intent = Intent(context, GameModeActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.data = Uri.parse(gameURL)
        intent.putExtra(GAME_URL, gameURL)
        intent.putExtra(LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_GAME_SHORTCUT.value, true)

        ShortcutUtils.requestPinShortcut(context, intent, gameName, gameURL, gameIcon)
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        recycler_view.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        recycler_view.visibility = View.VISIBLE
        no_result_view.visibility = View.GONE
    }

    private fun showErrorView() {
        spinner.visibility = View.GONE
        recycler_view.visibility = View.GONE
        no_result_view.visibility = View.VISIBLE
    }

    private fun showRecentPlayedSpotlight() {
        Looper.myQueue().addIdleHandler {
            activity?.let { activity ->
                if (isStateSaved) {
                    return@let
                }

                recycler_view.scrollToPosition(0)
                val gameCategoryView = recycler_view.layoutManager?.findViewByPosition(1)
                val gameListView = gameCategoryView?.findViewById<RecyclerView>(R.id.game_list)
                val gameItemView = gameListView?.layoutManager?.findViewByPosition(0)

                gameItemView?.let {
                    setSpotlightStatusBarColor()
                    recentPlayedSpotlightDialog = DialogUtils.showGameSpotlight(activity, it, DialogInterface.OnDismissListener {
                        restoreStatusBarColor()
                        instantGamesViewModel.onRecentPlayedSpotlightDismissed()
                    }, View.OnClickListener {
                        instantGamesViewModel.onRecentPlayedSpotlightButtonClicked()
                    })
                }
            }
            return@addIdleHandler false
        }
    }

    private fun closeRecentPlayedSpotlight() {
        if (::recentPlayedSpotlightDialog.isInitialized) {
            recentPlayedSpotlightDialog.dismiss()
        }
    }

    private fun restoreStatusBarColor() {
        activity?.window?.statusBarColor = Color.TRANSPARENT
    }

    private fun setSpotlightStatusBarColor() {
        activity?.let {
            it.window.statusBarColor = ContextCompat.getColor(it, R.color.paletteBlack50)
        }
    }

    companion object {
        private const val GAME_URL = "url"

        fun newInstance() = InstantGameFragment()
    }
}