package org.mozilla.rocket.msrp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_redeem.coupon_code
import kotlinx.android.synthetic.main.fragment_redeem.coupon_root
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.msrp.data.RedeemResult
import org.mozilla.rocket.msrp.data.RewardCouponDoc
import org.mozilla.rocket.widget.FxToast
import javax.inject.Inject

class RedeemFragment : Fragment() {

    private lateinit var viewModel: MissionViewModel

    @Inject
    lateinit var missionViewModelCreator: Lazy<MissionViewModel>

    fun create(url: String) = RedeemFragment().apply {
        arguments = Bundle().apply {
            putString(ARGUMENT_URL, url)
        }
    }

    // I'm not sure the best place to call `appComponent().inject(this)`.
    // I guess as long as it's earlier then the usage it's fine.
    override fun onAttach(context: Context) {
        appComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_redeem, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = getActivityViewModel(missionViewModelCreator)

        val redeemUrl =
            arguments?.getString(ARGUMENT_URL)
                ?: "https://rocket-dev01.appspot.com/api/v1/redeem/mission_daily?mid=AZ19ZOreJer4cAZMilrQ" // for development
//                throw IllegalStateException("Use UI to navigate to RedeemFragment")

        // consume all touch events to prevent from clicking through
        coupon_root.setOnTouchListener { _, _ -> true }

        tryRedeem(redeemUrl)

        observeRedeemResult()
    }

    private fun tryRedeem(redeemUrl: String) {
        viewModel.redeem(redeemUrl)
    }

    private fun observeRedeemResult() {
        viewModel.redeemResult.observe(viewLifecycleOwner, Observer { redeemResult ->
            updateUi(redeemResult)
        })
    }

    private fun updateUi(redeemResult: RedeemResult?) {

        when (redeemResult) {
            null -> {
            } // init state,
            is RedeemResult.Success -> showSuccessUi(redeemResult.rewardCouponDoc)
            is RedeemResult.UsedUp -> toast(redeemResult.message)
            is RedeemResult.NotReady -> toast(redeemResult.message)
            is RedeemResult.Failure -> toast(redeemResult.message)
            is RedeemResult.NotLogin -> {
                toast(redeemResult.message)
                // TODO: Evan
//                ScreenNavigator.get(context).addFxLogin()
            }
        }
    }

    private fun showSuccessUi(rewardCouponDoc: RewardCouponDoc) {
        coupon_code.text = rewardCouponDoc.code
//        rd_text.text = rewardCouponDoc.title
        // TODO: show content..etc
    }

    private fun toast(message: String) {
        FxToast.show(requireContext(), message, Toast.LENGTH_LONG)
        fragmentManager?.popBackStack() // remove self
    }

    companion object {
        private const val ARGUMENT_URL = "REDEEM_URL"
    }
}