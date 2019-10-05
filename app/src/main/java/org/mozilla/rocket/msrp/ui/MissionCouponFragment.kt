package org.mozilla.rocket.msrp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_mission_coupon.coupon_code
import kotlinx.android.synthetic.main.fragment_mission_coupon.coupon_copy_btn
import kotlinx.android.synthetic.main.fragment_mission_coupon.coupon_expiration
import kotlinx.android.synthetic.main.fragment_mission_coupon.coupon_go_shopping_btn
import kotlinx.android.synthetic.main.fragment_mission_coupon.image
import kotlinx.android.synthetic.main.fragment_mission_coupon.loading_view
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.appContext
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.extension.showToast
import javax.inject.Inject

class MissionCouponFragment : Fragment() {

    private val safeArgs: MissionCouponFragmentArgs by navArgs()
    private val mission by lazy { safeArgs.mission }
    private lateinit var viewModel: MissionCouponViewModel

    @Inject
    lateinit var missionCouponViewModelCreator: Lazy<MissionCouponViewModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        viewModel = getViewModel(missionCouponViewModelCreator)
        viewModel.init(mission)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_mission_coupon, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        bindData()
        observeAction()
    }

    private fun initViews() {
        coupon_copy_btn.setOnClickListener {
            viewModel.onCopyButtonClicked(coupon_code.text.toString())
        }
        coupon_go_shopping_btn.setOnClickListener {
            viewModel.onGoShoppingButtonClicked()
        }
    }

    private fun bindData() {
        viewModel.expirationTime.observe(this, Observer { timeText ->
            coupon_expiration.text = getString(R.string.msrp_reward_challenge_expire, timeText)
        })
        viewModel.missionImage.observe(this, Observer { url ->
            Glide.with(requireContext())
                    .load(url)
                    .apply(RequestOptions().apply { transforms(CircleCrop()) })
                    .into(image)
        })
        viewModel.couponCode.observe(this, Observer { couponCode ->
            coupon_code.text = couponCode
        })
        viewModel.isLoading.observe(this, Observer {
            loading_view.isVisible = it
        })
    }

    private fun observeAction() {
        viewModel.showToast.observe(this, Observer {
            appContext().showToast(it)
        })
        viewModel.copyToClipboard.observe(this, Observer { text ->
            copyToClipboard(COUPON_COPY_LABEL, text)
        })
        viewModel.openShoppingPage.observe(this, Observer {
            openShoppingPage()
        })
    }

    private fun copyToClipboard(label: String, text: String) {
        getSystemService(appContext(), ClipboardManager::class.java)?.run {
            primaryClip = ClipData.newPlainText(label, text)
        }
    }

    private fun openShoppingPage() {
        startActivity(ShoppingActivity.getStartIntent(requireContext()))
    }

    companion object {
        private const val COUPON_COPY_LABEL = "coupon"
    }
}