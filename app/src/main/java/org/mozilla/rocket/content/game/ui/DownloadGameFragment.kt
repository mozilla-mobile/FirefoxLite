package org.mozilla.rocket.content.game.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_game.*
import org.mozilla.focus.R
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
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

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var downloadGameViewModel: DownloadGameViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var adapter: DelegateAdapter
    private lateinit var permissionHandler: PermissionHandler

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String?, actionId: Int, params: Parcelable?) {
                this@DownloadGameFragment.context?.also {
                    val download = params as Download

                    if (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        // We do have the permission to write to the external storage. Proceed with the download.
                        queueDownload(download)
                    }
                }
            }

            fun actionDownloadGranted(parcelable: Parcelable?) {
                val download = parcelable as Download
                queueDownload(download)
            }

            override fun doActionGranted(permission: String?, actionId: Int, params: Parcelable?) {
                actionDownloadGranted(params)
            }

            override fun doActionSetting(permission: String?, actionId: Int, params: Parcelable?) {
                actionDownloadGranted(params)
            }

            override fun doActionNoPermission(
                permission: String?,
                actionId: Int,
                params: Parcelable?
            ) {
            }

            override fun makeAskAgainSnackBar(actionId: Int): Snackbar {
                activity?.also {
                    return PermissionHandler.makeAskAgainSnackBar(
                        this@DownloadGameFragment,
                        it.findViewById(R.id.container),
                        R.string.permission_toast_storage
                    )
                }
                throw IllegalStateException("No activity to show SnackBar.")
            }

            override fun permissionDeniedToast(actionId: Int) {
                Toast.makeText(getContext(), R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show()
            }

            override fun requestPermissions(actionId: Int) {
                this@DownloadGameFragment.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
            }

            private fun queueDownload(download: Download?) {
                activity?.let { activity ->
                    download?.let {
                        EnqueueDownloadTask(activity, it, download.url).execute()
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionHandler.onRequestPermissionsResult(context, requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        downloadGameViewModel = getActivityViewModel(downloadGameViewModelCreator)
        telemetryViewModel = getActivityViewModel(telemetryViewModelCreator)
        downloadGameViewModel.requestGameList()
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
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel, TelemetryWrapper.Extra_Value.DOWNLOAD_GAME, telemetryViewModel))
                add(GameCategory::class, R.layout.item_game_category, DownloadGameCategoryAdapterDelegate(downloadGameViewModel, telemetryViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@DownloadGameFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        downloadGameViewModel.downloadGameItems.observe(viewLifecycleOwner, Observer {
            adapter.setData(it)
            telemetryViewModel.updateVersionId(TelemetryWrapper.Extra_Value.DOWNLOAD_GAME, downloadGameViewModel.versionId)
        })
    }

    private fun bindPageState() {
        downloadGameViewModel.isDataLoading.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is DownloadGameViewModel.State.Idle -> showContentView()
                is DownloadGameViewModel.State.Loading -> showLoadingView()
                is DownloadGameViewModel.State.Error -> showErrorView()
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
                            action.telemetryData.copy(vertical = TelemetryWrapper.Extra_Value.GAME, versionId = downloadGameViewModel.versionId)))
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
                            action.telemetryData.copy(vertical = TelemetryWrapper.Extra_Value.GAME, versionId = downloadGameViewModel.versionId)))
                    }
                }
            }
        })

        downloadGameViewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is DownloadGameViewModel.GameAction.Install -> {
                    // val install: DownloadGameViewModel.GameAction.Install = event
                    // install a APK
                    val install: DownloadGameViewModel.GameAction.Install = event
                    downloadGame(install.url)
                }
                is DownloadGameViewModel.GameAction.Launch -> {
                    val launch: DownloadGameViewModel.GameAction.Launch = event
                    context?.let {
                        startActivity(it.packageManager.getLaunchIntentForPackage(launch.packageName))
                    }
                }
                is DownloadGameViewModel.GameAction.Share -> {
                    val share: DownloadGameViewModel.GameAction.Share = event
                    context?.let {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.gaming_vertical_share_message), share.url))
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

    private fun downloadGame(downloadURL: String) {
        context?.let {
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
            permissionHandler.tryAction(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ACTION_DOWNLOAD,
                download
            )
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
        private const val ACTION_DOWNLOAD = 0

        fun newInstance() = DownloadGameFragment()
    }
}