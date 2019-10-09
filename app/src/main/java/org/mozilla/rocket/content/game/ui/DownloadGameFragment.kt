package org.mozilla.rocket.content.game.ui

import android.Manifest
import android.content.Intent
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
import org.mozilla.rocket.content.game.ui.adapter.DownloadGameCategoryAdapterDelegate
import org.mozilla.rocket.content.game.ui.model.GameCategory
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.tabs.web.Download
import javax.inject.Inject

class DownloadGameFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var downloadGameViewModelCreator: Lazy<DownloadGameViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var downloadGameViewModel: DownloadGameViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        downloadGameViewModel = getActivityViewModel(downloadGameViewModelCreator)
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
        initNoResultView()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                add(GameCategory::class, R.layout.item_game_category, DownloadGameCategoryAdapterDelegate(downloadGameViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@DownloadGameFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        downloadGameViewModel.downloadGameItems.observe(this@DownloadGameFragment, Observer {
            adapter.setData(it)
        })
    }

    private fun bindPageState() {
        downloadGameViewModel.isDataLoading.observe(this@DownloadGameFragment, Observer { state ->
            when (state) {
                is DownloadGameViewModel.State.Idle -> showContentView()
                is DownloadGameViewModel.State.Loading -> showLoadingView()
                is DownloadGameViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeGameAction() {
        runwayViewModel.openRunway.observe(this, Observer { linkUrl ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, linkUrl))
            }
        })

        downloadGameViewModel.event.observe(this, Observer { event ->
            when (event) {
                is DownloadGameViewModel.GameAction.Install -> {
                    // val install: DownloadGameViewModel.GameAction.Install = event
                    // install a APK
                    val install: DownloadGameViewModel.GameAction.Install = event
                    downloadGame(install.url)
                }
                is DownloadGameViewModel.GameAction.Share -> {
                    val share: DownloadGameViewModel.GameAction.Share = event
                    context?.let {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.gaming_vertical_share_message, share.url))
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(sendIntent, null))
                    }
                }
            }
        })
    }

    private fun initNoResultView() {
        no_result_view.setButtonOnClickListener(View.OnClickListener {
            downloadGameViewModel.onRetryButtonClicked()
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
        context?.let {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // We do have the permission to write to the external storage. Proceed with the download.
                val contentDisposition = ""
                val mimeType = "application/vnd.android.package-archive"
                val name = URLUtil.guessFileName(downloadURL, contentDisposition, mimeType)
                val download = Download(
                    downloadURL,
                    name,
                    WebViewProvider.getUserAgentString(it),
                    contentDisposition,
                    mimeType,
                    0,
                    false
                )

                queueDownload(download, downloadURL)
            }
        }
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

    companion object {
        fun newInstance() = DownloadGameFragment()
    }
}