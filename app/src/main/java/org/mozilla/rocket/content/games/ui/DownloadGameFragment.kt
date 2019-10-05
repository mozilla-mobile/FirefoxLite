package org.mozilla.rocket.content.games.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_game.*
import org.mozilla.focus.R
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameCategoryAdapterDelegate
import org.mozilla.rocket.content.games.ui.adapter.GameType
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.tabs.web.Download
import javax.inject.Inject

class DownloadGameFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var gamesViewModelCreator: Lazy<GamesViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: DelegateAdapter
    private lateinit var gameType: GameType

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        gamesViewModel = getActivityViewModel(gamesViewModelCreator)
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
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                add(GameCategory::class, R.layout.item_game_category, GameCategoryAdapterDelegate(gamesViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@DownloadGameFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        gamesViewModel.downloadGameItems.observe(this@DownloadGameFragment, Observer {
            adapter.setData(it)
        })
    }

    private fun bindPageState() {
        gamesViewModel.isDataLoading.observe(this@DownloadGameFragment, Observer { state ->
            when (state) {
                is GamesViewModel.State.Idle -> showContentView()
                is GamesViewModel.State.Loading -> showLoadingView()
                is GamesViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeGameAction() {
        gamesViewModel.event.observe(this, Observer { event ->
            when (event) {
                is GamesViewModel.GameAction.Install -> {
                    // val install: GamesViewModel.GameAction.Install = event
                    // install a APK
                    val install: GamesViewModel.GameAction.Install = event
                    downloadGame(install.url)
                }
                is GamesViewModel.GameAction.OpenLink -> {
                    val openLink: GamesViewModel.GameAction.OpenLink = event
                    context?.let {
                        startActivity(ContentTabActivity.getStartIntent(it, openLink.url))
                    }
                }
            }
        })
    }

    private fun queueDownload(download: Download?, url: String) {
        val activity = activity
        if (activity == null || download == null) {
            return
        }

        EnqueueDownloadTask(activity, download, url).execute()
    }

    private fun downloadGame(downloadURL: String) {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // We do have the permission to write to the external storage. Proceed with the download.
            val url = downloadURL
            val contentDisposition = ""
            val mimetype = "application/vnd.android.package-archive"
            val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val download = Download(
                url,
                name,
                WebViewProvider.getUserAgentString(context),
                contentDisposition,
                mimetype,
                0,
                false
            )

            queueDownload(download, url)
        }
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
    }

    private fun showErrorView() {
        TODO("not implemented")
    }

    companion object {
        fun newInstance() = DownloadGameFragment()
    }
}