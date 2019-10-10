package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_travel_city.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.ExploreIgAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.ExploreVideoAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.ExploreWikiAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel
import javax.inject.Inject

class TravelCityActivity : BaseActivity() {

    @Inject
    lateinit var travelCityViewModelCreator: Lazy<TravelCityViewModel>

    private lateinit var travelCityViewModel: TravelCityViewModel
    private lateinit var name: String
    private lateinit var detailAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        travelCityViewModel = getViewModel(travelCityViewModelCreator)
        setContentView(R.layout.activity_travel_city)

        name = intent?.extras?.getString(EXTRA_NAME) ?: ""

        initToolBar()
        initDetail()
        bindListData()
        initExploreActions()
    }

    override fun applyLocale() = Unit

    private fun initDetail() {
        detailAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                // TODO: add adapter delegates
                add(SectionHeaderUiModel::class, R.layout.item_section_header, SectionHeaderAdapterDelegate(travelCityViewModel))
                add(IgUiModel::class, R.layout.item_travel_detail_ig, ExploreIgAdapterDelegate(travelCityViewModel))
                add(VideoUiModel::class, R.layout.item_travel_detail_video, ExploreVideoAdapterDelegate(travelCityViewModel))
                add(WikiUiModel::class, R.layout.item_travel_detail_wiki, ExploreWikiAdapterDelegate(travelCityViewModel))
            }
        )
        city_details.apply {
            adapter = detailAdapter
        }
    }

    private fun bindListData() {
        travelCityViewModel.items.observe(this, Observer {
            detailAdapter.setData(it)
        })
        travelCityViewModel.getLatestItems(name)
    }

    private fun initToolBar() {
        toolbar.title = name
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        favorite_button.setOnClickListener {
            // TODO: view model toggle favorite
            it.isSelected = !it.isSelected
            if (it.isSelected) showBucketListAddedSnackbar()
        }
        refresh_button.setOnClickListener {
            travelCityViewModel.getLatestItems(name)
        }
    }

    private fun initExploreActions() {

        travelCityViewModel.openLinkUrl.observe(this, Observer { linkUrl ->
            startActivity(ContentTabActivity.getStartIntent(this@TravelCityActivity, linkUrl))
        })
    }

    private fun showBucketListAddedSnackbar() {
        Snackbar.make(container, R.string.travel_detail_bucket_list_saved, Snackbar.LENGTH_LONG).apply {
            setAction(R.string.travel_detail_bucket_list_saved_view) {
                // TODO: show bucket list
            }
        }.show()
    }

    private fun bindPageState() {
        travelCityViewModel.isDataLoading.observe(this, Observer { state ->
            when (state) {
                is TravelCityViewModel.State.Idle -> showContentView()
                is TravelCityViewModel.State.Loading -> showLoadingView()
                is TravelCityViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        city_details.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        city_details.visibility = View.VISIBLE
    }

    private fun showErrorView() {
        spinner.visibility = View.GONE
        city_details.visibility = View.GONE
        // TODO: show error view?
    }

    companion object {
        private const val EXTRA_NAME = "name"
        fun getStartIntent(context: Context, name: String) =
                Intent(context, TravelCityActivity::class.java).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.putExtra(EXTRA_NAME, name)
                }
    }
}
