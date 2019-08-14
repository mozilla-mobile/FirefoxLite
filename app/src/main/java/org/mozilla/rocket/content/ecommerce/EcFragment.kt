package org.mozilla.rocket.content.ecommerce

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.banner.BannerAdapter
import org.mozilla.banner.BannerConfigViewModel
import org.mozilla.banner.TelemetryListener
import org.mozilla.focus.R
import org.mozilla.focus.home.BannerHelper
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.data.ShoppingLink
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.portal.ContentFeature.Companion.TYPE_COUPON
import org.mozilla.rocket.content.portal.ContentFeature.Companion.TYPE_KEY
import org.mozilla.rocket.content.portal.ContentFeature.Companion.TYPE_TICKET
import javax.inject.Inject

/**
 * Fragment that display the content for [ShoppingLink]s and [Coupon]s
 *
 */
class EcFragment : Fragment() {

    companion object {
        fun newInstance(feature: Int): EcFragment {
            val args = Bundle().apply {
                putInt(TYPE_KEY, feature)
            }
            return EcFragment().apply { arguments = args }
        }
    }

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var chromeViewModel: ChromeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        when (arguments?.getInt(TYPE_KEY)) {
            TYPE_TICKET -> {
                return inflater.inflate(R.layout.content_tab_shoppinglink, container, false).apply {

                    val recyclerView = findViewById<RecyclerView>(R.id.ct_shoppinglink_list)
                    val shoppinglinkAdapter = ShoppingLinkAdapter()
                    recyclerView?.layoutManager = GridLayoutManager(context, 2)
                    recyclerView?.adapter = shoppinglinkAdapter

                    val ecommerceShoppingLinks = AppConfigWrapper.getEcommerceShoppingLinks()
                    ecommerceShoppingLinks.add(
                        ShoppingLink(
                            "",
                            "footer",
                            "",
                            ""
                        )
                    )
                    shoppinglinkAdapter.submitList(ecommerceShoppingLinks)
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}