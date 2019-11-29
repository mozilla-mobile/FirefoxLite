package org.mozilla.rocket.content.travel.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_travel_city.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.ExploreIgAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.ExploreLoadingAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.ExploreVideoAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.ExploreWikiAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.HotelAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.HotelUiModel
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.LoadingUiModel
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderUiModel
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter.Tab.BucketList
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel
import javax.inject.Inject

class TravelCityActivity : BaseActivity() {

    @Inject
    lateinit var travelCityViewModelCreator: Lazy<TravelCityViewModel>

    private lateinit var travelCityViewModel: TravelCityViewModel
    private lateinit var detailAdapter: DelegateAdapter
    private lateinit var onboardingSpotlightDialog: Dialog
    private lateinit var city: BaseCityData

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        travelCityViewModel = getViewModel(travelCityViewModelCreator)
        setContentView(R.layout.activity_travel_city)

        city = intent?.extras?.getParcelable(EXTRA_CITY) ?: BaseCityData("", "", "", "", "")

        initToolBar()
        initSnackBar()
        initDetail()
        bindPageState()
        bindCityData()
        initExploreActions()
        initOnboardingSpotlight(city.name)
    }

    private fun initOnboardingSpotlight(name: String) {
        travelCityViewModel.showOnboardingSpotlight.observe(this, Observer {
            showOnboardingSpotlight(name)
        })
    }

    private fun showOnboardingSpotlight(name: String) {
        favorite_button.post {
            window.statusBarColor = ContextCompat.getColor(this, R.color.paletteBlack50)
            onboardingSpotlightDialog = DialogUtils.showTravelSpotlight(this, favorite_button, name, {
                window?.statusBarColor = Color.TRANSPARENT
            }) {
                if (::onboardingSpotlightDialog.isInitialized) {
                    onboardingSpotlightDialog.dismiss()
                }
            }
        }
    }

    override fun applyLocale() = Unit

    private fun initToolBar() {
        toolbar.title = city.name
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        favorite_button.setOnClickListener {
            travelCityViewModel.onFavoriteToggled(city, it.isSelected)
        }
        refresh_button.setOnClickListener {
            travelCityViewModel.getLatestItems(this@TravelCityActivity, city.name, city.id, city.type)
        }
    }

    private fun initSnackBar() {
        travelCityViewModel.showSnackBar.observe(this, Observer {
            showBucketListAddedSnackbar()
        })
    }

    private fun initDetail() {
        detailAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                // TODO: add adapter delegates
                add(SectionHeaderUiModel::class, R.layout.item_section_header, SectionHeaderAdapterDelegate(travelCityViewModel))
                add(IgUiModel::class, R.layout.item_travel_detail_ig, ExploreIgAdapterDelegate(travelCityViewModel))
                add(VideoUiModel::class, R.layout.item_travel_detail_video, ExploreVideoAdapterDelegate(travelCityViewModel))
                add(WikiUiModel::class, R.layout.item_travel_detail_wiki, ExploreWikiAdapterDelegate(travelCityViewModel))
                add(HotelUiModel::class, R.layout.item_hotel, HotelAdapterDelegate(travelCityViewModel))
                add(LoadingUiModel::class, R.layout.item_loading, ExploreLoadingAdapterDelegate())
            }
        )
        city_details.apply {
            adapter = detailAdapter
        }

        (city_details.layoutManager as LinearLayoutManager).let {
            city_details.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val totalItemCount = it.itemCount
                    val visibleItemCount = it.childCount
                    val firstVisibleItem = it.findFirstVisibleItemPosition()
                    travelCityViewModel.onDetailItemScrolled(firstVisibleItem, visibleItemCount, totalItemCount, dy > 0)
                }
            })
        }
    }

    private fun bindCityData() {
        travelCityViewModel.isInBucketList.observe(this, Observer {
            favorite_button.isSelected = it
        })
        travelCityViewModel.checkIsInBucketList(city.id)
        travelCityViewModel.items.observe(this, Observer {
            detailAdapter.setData(it)
        })
        travelCityViewModel.getLatestItems(this@TravelCityActivity, city.name, city.id, city.type)
    }

    private fun initExploreActions() {

        travelCityViewModel.openLinkUrl.observe(this, Observer { linkUrl ->
            startActivity(ContentTabActivity.getStartIntent(this@TravelCityActivity, linkUrl))
        })
    }

    private fun showBucketListAddedSnackbar() {
        Snackbar.make(container, R.string.travel_snack_bar_saved_message, Snackbar.LENGTH_LONG).apply {
            setAction(R.string.travel_snack_bar_button) {
                startActivity(TravelActivity.getStartIntent(it.context, BucketList).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) })
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
        private const val EXTRA_CITY = "city"
        fun getStartIntent(context: Context, city: BaseCityData) =
                Intent(context, TravelCityActivity::class.java).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.putExtra(EXTRA_CITY, city)
                }
    }
}
