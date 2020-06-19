package org.mozilla.rocket.home.topsites.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_add_new_top_sites.recycler_view
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class AddNewTopSitesFragment : Fragment() {

    @Inject
    lateinit var addNewTopSitesViewModelCreator: Lazy<AddNewTopSitesViewModel>

    private lateinit var addNewTopSitesViewModel: AddNewTopSitesViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        addNewTopSitesViewModel = getActivityViewModel(addNewTopSitesViewModelCreator)
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
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(RecommendedSitesUiCategory::class, R.layout.item_recommended_sites_category, RecommendedSitesCategoryAdapterDelegate())
                add(Site.UrlSite.FixedSite::class, R.layout.item_recommended_site, RecommendedSitesAdapterDelegate(addNewTopSitesViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@AddNewTopSitesFragment.adapter
        }

        initSpanSizeLookup()
        initItemDecoration()
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

    private fun initItemDecoration() {
        recycler_view.addItemDecoration(
            DefaultGridSpacingItemDecoration(
                recycler_view.context.resources.getDimensionPixelOffset(R.dimen.recommended_sites_spacing),
                recycler_view.context.resources.getDimensionPixelOffset(R.dimen.common_margin_m1),
                recycler_view.context.resources.getDimensionPixelOffset(R.dimen.common_margin_m2)
            )
        )
    }

    companion object {
        const val TAG = "AddNewTopSitesFragment"

        fun newInstance() = AddNewTopSitesFragment()
    }

    private class DefaultGridSpacingItemDecoration(
        private val colSpacing: Int,
        private val rowSpacing: Int,
        private val edgePadding: Int
    ) : RecyclerView.ItemDecoration() {

        private var spanCount = -1

        override fun getItemOffsets(outRect: Rect, view: View, recyclerView: RecyclerView, state: RecyclerView.State) {
            val layoutManager = recyclerView.layoutManager as GridLayoutManager

            // cache the span count
            if (spanCount == -1) {
                spanCount = layoutManager.spanCount
            }
            val position: Int = recyclerView.getChildAdapterPosition(view)
            val colSpans = layoutManager.spanSizeLookup.getSpanSize(position)
            val colSpanIndex = layoutManager.spanSizeLookup.getSpanIndex(position, spanCount)
            val rowSpanIndex = layoutManager.spanSizeLookup.getSpanGroupIndex(position, spanCount)

            if (colSpans > 1) {
                return
            }

            // the leftmost one in row -> set left padding as edge padding
            if (colSpanIndex == 0) {
                outRect.left = edgePadding
            }

            // the rightmost one in row -> set right padding as edge padding
            if (colSpanIndex + colSpans == spanCount) {
                outRect.right = edgePadding
            }

            // row contains more than 1 column
            if (colSpans != spanCount) {
                // not the rightmost one in row -> set right padding as (column spacing)/2
                if (colSpanIndex + colSpans != spanCount) {
                    outRect.right = colSpacing / 2
                }

                // not the leftmost one in row -> set left padding as (column spacing)/2
                if (colSpanIndex != 0) {
                    outRect.left = colSpacing / 2
                }
            }

            // adjust top
            if (rowSpanIndex != 0) {
                outRect.top = rowSpacing
            }
        }
    }
}
