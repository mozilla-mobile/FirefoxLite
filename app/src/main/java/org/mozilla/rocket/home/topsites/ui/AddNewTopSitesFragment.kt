package org.mozilla.rocket.home.topsites.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_add_new_top_sites.recycler_view
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class AddNewTopSitesFragment : Fragment() {

    @Inject
    lateinit var addNewTopSitesViewModelCreator: Lazy<AddNewTopSitesViewModel>

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var addNewTopSitesViewModel: AddNewTopSitesViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        addNewTopSitesViewModel = getActivityViewModel(addNewTopSitesViewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)

        addNewTopSitesViewModel.requestRecommendedSitesList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_new_top_sites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
    }

    private fun initRecyclerView() {
        val specifiedFaviconBgColors = getFaviconBgColorsFromResource(recycler_view.context.applicationContext)
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(RecommendedSitesUiCategory::class, R.layout.item_recommended_sites_category, RecommendedSitesCategoryAdapterDelegate())
                add(Site.UrlSite.FixedSite::class, R.layout.item_top_site, SiteAdapterDelegate(addNewTopSitesViewModel, chromeViewModel, specifiedFaviconBgColors))
            }
        )
        recycler_view.apply {
            adapter = this@AddNewTopSitesFragment.adapter
        }

        initSpanSizeLookup()
    }

    private fun bindListData() {
        addNewTopSitesViewModel.recommendedSitesItems.observe(viewLifecycleOwner, Observer {
            adapter.setData(it.items)
        })
    }

    private fun initSpanSizeLookup() {
        val layoutManager = recycler_view.layoutManager as GridLayoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return recycler_view.adapter?.let {
                    when (it.getItemViewType(position)) {
                        R.layout.item_recommended_sites_category -> 4
                        else -> 1
                    }
                } ?: 0
            }
        }
        layoutManager.spanSizeLookup.isSpanIndexCacheEnabled = true
    }

    companion object {
        const val TAG = "AddNewTopSitesFragment"

        fun newInstance() = AddNewTopSitesFragment()
    }
}
