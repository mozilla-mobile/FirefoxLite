package org.mozilla.rocket.content.travel.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_travel_bucket_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.ui.VerticalSpaceItemDecoration
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityItem

class TravelBucketListFragment : Fragment() {

    private lateinit var bucketListAdapter: DelegateAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_travel_bucket_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBucketList()
        bindExploreData()
    }

    private fun initBucketList() {
        bucketListAdapter = DelegateAdapter(
                AdapterDelegatesManager().apply {
                    add(BucketListCityItem::class, R.layout.item_bucket_list, BucketListCityAdapterDelegate())
                }
        )

        bucket_list_recycler_view.apply {
            val spaceWidth = resources.getDimensionPixelSize(R.dimen.card_space_width)
            addItemDecoration(VerticalSpaceItemDecoration(spaceWidth))

            adapter = bucketListAdapter
        }
    }

    private fun bindExploreData() {
        //TODO bind view model for data
        bucketListAdapter.setData(DEFAULT_CITY_ITEMS)
    }

    companion object {

        @JvmStatic
        val DEFAULT_CITY_ITEMS = listOf(
            BucketListCityItem("Bali","https://rukminim1.flixcart.com/flap/1440/640/image/c17480802f3886a9.jpg?q=90","", true),
            BucketListCityItem("Los Angeles","https://rukminim1.flixcart.com/flap/1440/640/image/69ec96c8387c70a1.jpg?q=90","", true),
            BucketListCityItem("Athens, Greece","https://rukminim1.flixcart.com/flap/1440/640/image/4ca2f501323d9f50.jpg?q=90","", false),
            BucketListCityItem("PingTung, Taiwan","https://images-eu.ssl-images-amazon.com/images/G/31/img17/AmazonDevices/2019/jupiter/bunk/mob._CB452464492_SY367_.jpg","", true)
        )
    }
}