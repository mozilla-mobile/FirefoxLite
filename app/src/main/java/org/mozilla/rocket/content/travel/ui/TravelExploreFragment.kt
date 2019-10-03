package org.mozilla.rocket.content.travel.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_travel_explore.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.travel.ui.adapter.*
import javax.inject.Inject

class TravelExploreFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var exploreAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_travel_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initExplore()
        bindExploreData()
    }

    private fun initExplore() {
        exploreAdapter = DelegateAdapter(
                AdapterDelegatesManager().apply {
                    add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                    add(CitySearch::class, R.layout.city_search, CitySearchAdapterDelegate())
                    add(CityCategory::class, R.layout.item_city_category, CityCategoryAdapterDelegate())
                }
        )
        explore_recycler_view.apply {
            adapter = exploreAdapter
        }
    }

    private fun bindExploreData() {
        //TODO bind view model for data
        exploreAdapter.setData(DEFAULT_EXPLORE_ITEMS)
    }


    companion object {

        @JvmStatic
        val DEFAULT_RUNWAY_ITEMS = listOf(
            RunwayItem("Flipkart", "https://rukminim1.flixcart.com/flap/1440/640/image/c17480802f3886a9.jpg?q=90", "https://www.flipkart.com/fashion-bbd19-sneak-peak-store?param=1", "", "b649cd4aedc352577f336dc2906ad06dfa5b8f77ff93ba572af1c2d3ecba1bae"),
            RunwayItem("Flipkart", "https://rukminim1.flixcart.com/flap/1440/640/image/69ec96c8387c70a1.jpg?q=90", "https://www.flipkart.com/big-billion-days-specials-store?param=198912388213", "", "2d8fde88942ced9feb0aedcb2d5cfdfcc0eac4abeec53ddf36dd94dfafd150b3"),
            RunwayItem("Flipkart", "https://rukminim1.flixcart.com/flap/1440/640/image/4ca2f501323d9f50.jpg?q=90", "https://www.flipkart.com/redmi-8a-matte-blue-32-gb/p/itmfhz4cztznu8kk?pid=MOBFKF98FFAAY8EB&pageUID=1569399524856", "", "a7348c33a51d07b9426ffd86f6830e72fc8c727992634ebed94916cca9d4c3b2"),
            RunwayItem("Amazon.in", "https://images-eu.ssl-images-amazon.com/images/G/31/img17/AmazonDevices/2019/jupiter/bunk/mob._CB452464492_SY367_.jpg", "https://www.amazon.in/b?_encoding=UTF8&ie=UTF8&node=18039422031&pf_rd_i=mobile&pf_rd_m=A1VBAL9TL5WCBF&pf_rd_p=499583d5-02db-46a9-aead-733aa186d7b4&pf_rd_r=JVNNAFTYH3FVB63ZEYRS&pf_rd_s=mobile-hero-slide-7&pf_rd_t=36701&ref_=Oct_Arh_mobile-hero-slide-7_01e923b6-b9f4-4bc8-8f87-7130ee4cd094", "", "64a5b385f82caaeb05565356d143a9b737bc29bb8342e30005ae2f3b50ad3b84")
        )

        @JvmStatic
        val DEFAULT_CITY_ITEMS_1 = listOf(
            CityItem("https://rukminim1.flixcart.com/image/352/352/jmkwya80/sports-action-camera/n/j/m/hero-7-hero-7-chdhx-701-rw-gopro-original-imaf9fegntmxfhfs.jpeg?q=70",
                    "https://www.flipkart.com/gopro-hero-7-sports-action-camera/p/itmf9fs3rufrgjep?pid=SAYF9FEPR2YM5NJM&srno=b_1_1&otracker=hp_omu_Best%2Bof%2BElectronic%2BDevices_3_22.dealCard.OMU_WKIUF7DK5SPE_18&otracker1=hp_omu_WHITELISTED_neo%2Fmerchandising_Best%2Bof%2BElectronic%2BDevices_NA_dealCard_cc_3_NA_view-all_18&lid=LSTSAYF9FEPR2YM5NJMQOAKDI&fm=organic&iid=b9be2242-f408-4eda-be3c-07c74a9a6cff.SAYF9FEPR2YM5NJM.SEARCH&ssid=63mjvm03ls0000001569399860597",
                    "Taipei"),
            CityItem("https://rukminim1.flixcart.com/image/714/857/jmz7csw0/slipper-flip-flop/s/a/z/19103309-11-puma-peacoat-white-limepunch-original-imaf9qzfmagzuqkm.jpeg?q=50",
                    "https://www.flipkart.com/puma-blink-duo-idp-slippers/p/itmf6ygsyavwa6tw?pid=SFFF6YGTRBKPHBXE&lid=LSTSFFF6YGTRBKPHBXED9ZAE5&marketplace=FLIPKART&srno=b_1_3&otracker=hp_omu_Fashion%2Bfor%2BTravel%2BLovers_4_20.dealCard.OMU_MH5VV8G8UAOR_16&otracker1=hp_omu_WHITELISTED_neo%2Fmerchandising_Fashion%2Bfor%2BTravel%2BLovers_NA_dealCard_cc_4_NA_view-all_16&fm=organic&iid=1474436e-9362-479e-9367-0c4c1d0f4ca7.SFFF6YGTRBKPHBXE.SEARCH&ppt=browse&ppn=browse&ssid=n8p977jbl03x03r41569399857284",
                    "Pingtung"),
            CityItem("https://rukminim1.flixcart.com/image/714/857/jmqmpow0-1/sunglass/t/2/w/one-size-fits-all-p429gr3-fastrack-original-imaf9kx9fuceyggz.jpeg?q=50",
                    "https://www.flipkart.com/fastrack-wayfarer-sunglasses/p/itmf79dqjqucq9f3?pid=SGLF79DQR8NQAT2W&lid=LSTSGLF79DQR8NQAT2WZTFJ3O&marketplace=FLIPKART&srno=b_1_2&otracker=hp_omu_Fashion%2Bfor%2BTravel%2BLovers_1_20.dealCard.OMU_YD5LDN6AP7_16&otracker1=hp_omu_WHITELISTED_neo%2Fmerchandising_Fashion%2Bfor%2BTravel%2BLovers_NA_dealCard_cc_1_NA_view-all_16&fm=organic&iid=40911f58-1136-4f4a-977a-296ecdadf662.SGLF79DQR8NQAT2W.SEARCH&ppt=browse&ppn=browse&ssid=lt026f5lnoq9enls1569399787144",
                    "New York")
        )

        @JvmStatic
        val DEFAULT_CITY_ITEMS_2 = listOf(
            CityItem("https://rukminim1.flixcart.com/image/714/857/jmqmpow0-1/sunglass/t/2/w/one-size-fits-all-p429gr3-fastrack-original-imaf9kx9fuceyggz.jpeg?q=50",
                    "https://www.flipkart.com/fastrack-wayfarer-sunglasses/p/itmf79dqjqucq9f3?pid=SGLF79DQR8NQAT2W&lid=LSTSGLF79DQR8NQAT2WZTFJ3O&marketplace=FLIPKART&srno=b_1_2&otracker=hp_omu_Fashion%2Bfor%2BTravel%2BLovers_1_20.dealCard.OMU_YD5LDN6AP7_16&otracker1=hp_omu_WHITELISTED_neo%2Fmerchandising_Fashion%2Bfor%2BTravel%2BLovers_NA_dealCard_cc_1_NA_view-all_16&fm=organic&iid=40911f58-1136-4f4a-977a-296ecdadf662.SGLF79DQR8NQAT2W.SEARCH&ppt=browse&ppn=browse&ssid=lt026f5lnoq9enls1569399787144",
                     "Bangkok"),
            CityItem("https://rukminim1.flixcart.com/image/352/352/j9it30w0/bulb/p/x/j/929001834413-2-philips-original-imaezatuwpm3xsbw.jpeg?q=70",
                    "https://www.flipkart.com/philips-8-5-w-round-b22-led-bulb/p/itmez6ff3qjygp3q?pid=BLBEZ6FFWZZZ8PXJ&lid=LSTBLBEZ6FFWZZZ8PXJGETXGV&marketplace=FLIPKART&spotlightTagId=BestsellerId_jhg%2Fyqn%2Ffeb&srno=b_1_2&otracker=hp_omu_Home%2BUtility_3_19.dealCard.OMU_ZGXHB6IXY6YQ_15&otracker1=hp_omu_WHITELISTED_neo%2Fmerchandising_Home%2BUtility_NA_dealCard_cc_3_NA_view-all_15&iid=ff46b9a5-d14f-4a0d-a0e9-3795068f0945.BLBEZ6FFWZZZ8PXJ.SEARCH&ppt=browse&ppn=browse&ssid=0tlf273hdvurrjsw1569399785657",
                    "Jakarta"),
            CityItem("https://rukminim1.flixcart.com/image/352/352/js0o9zk0/shoe-rack/m/q/f/4-step-black-benesta-original-imaevhkqgcxzhx3m.jpeg?q=70",
                    "https://www.flipkart.com/benesta-metal-shoe-stand/p/itmevhpfpsh28afe?pid=SHKEVHPF9MM6B6BH&lid=LSTSHKEVHPF9MM6B6BHRB42Y6&marketplace=FLIPKART&srno=b_1_16&otracker=hp_omu_Budget%2BFriendly%2BFurniture_4_16.dealCard.OMU_K3EAM39C2JMP_12&otracker1=hp_omu_WHITELISTED_neo%2Fmerchandising_Budget%2BFriendly%2BFurniture_NA_dealCard_cc_4_NA_view-all_12&fm=organic&iid=ca7505c1-23e4-41ca-af78-aea436c087bc.SHKEVHPF9MM6B6BH.SEARCH&ppt=browse&ppn=browse",
                    "Bangdung")
        )

        @JvmStatic
        val DEFAULT_EXPLORE_ITEMS = listOf(
            CitySearch("Which city would you like to visit?","Try “Bali”"),
            Runway("banner","banner",4, DEFAULT_RUNWAY_ITEMS),
            CityCategory("Best cities to visit this summer",DEFAULT_CITY_ITEMS_1),
            CityCategory("Travel on budget",DEFAULT_CITY_ITEMS_2)
        )
    }

}