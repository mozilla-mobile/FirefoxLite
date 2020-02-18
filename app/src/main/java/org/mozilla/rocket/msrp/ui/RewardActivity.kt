package org.mozilla.rocket.msrp.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import org.mozilla.focus.R
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.rocket.fxa.FxLoginFragment
import org.mozilla.rocket.fxa.FxLoginFragment.Companion.STATUS_CODE_FINAL_WARNING
import org.mozilla.rocket.fxa.FxLoginFragment.Companion.STATUS_CODE_WARNING
import org.mozilla.rocket.msrp.data.Mission
import kotlin.properties.Delegates

class RewardActivity : AppCompatActivity(), NavHostActivity, FxLoginFragment.OnLoginCompleteListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward)

        intent.extras?.let {
            parseDeepLink(it)
        }
    }

    private fun parseDeepLink(bundle: Bundle): Boolean {
        when (bundle.getString(EXTRA_DEEP_LINK) ?: return false) {
            DEEP_LINK_MISSION_DETAIL_PAGE -> openMissionRedeemPage(bundle.getParcelable(EXTRA_MISSION)!!)
        }

        return true
    }

    override fun onLoginSuccess(requestCode: Int, jwt: String, isDisabled: Boolean, statusCode: Int) {
        when {
            isDisabled -> {
                DialogUtils.showAccountDisabledDialog(this, DialogInterface.OnDismissListener {
                    finish()
                })
            }
            statusCode == STATUS_CODE_WARNING -> DialogUtils.showLoginMultipleTimesWarningDialog(this)
            statusCode == STATUS_CODE_FINAL_WARNING -> DialogUtils.showLoginMultipleTimesFinalWarningDialog(this)
        }
        navigateBackWithResult(
            Bundle().apply {
                putInt(MissionDetailFragment.RESULT_INT_REQUEST_CODE, requestCode)
                putString(MissionDetailFragment.RESULT_STR_JWT, jwt)
            }
        )
    }

    override fun onLoginFailure() {
        Toast.makeText(applicationContext, R.string.msrp_reward_challenge_error, Toast.LENGTH_LONG).show()
    }

    override fun navigateBackWithResult(result: Bundle) {
        val childFragmentManager = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager
        var backStackListener: FragmentManager.OnBackStackChangedListener by Delegates.notNull()
        backStackListener = FragmentManager.OnBackStackChangedListener {
            (childFragmentManager?.fragments?.get(0) as NavigationResult).onNavigationResult(result)
            childFragmentManager.removeOnBackStackChangedListener(backStackListener)
        }
        childFragmentManager?.addOnBackStackChangedListener(backStackListener)
        findNavController(R.id.nav_host_fragment).popBackStack()
    }

    private fun openMissionRedeemPage(mission: Mission) {
        findNavController(R.id.nav_host_fragment)
                .navigate(RewardFragmentDirections.actionRewardDestToMissionDetailDest(mission))
    }

    sealed class DeepLink(val name: String) {
        data class MissionDetailPage(val mission: Mission) : DeepLink(DEEP_LINK_MISSION_DETAIL_PAGE)
    }

    companion object {
        private const val EXTRA_DEEP_LINK = "extra_deep_link"
        private const val EXTRA_MISSION = "extra_mission"
        private const val DEEP_LINK_MISSION_DETAIL_PAGE = "deep_link_mission_detail_page"

        fun getStartIntent(context: Context): Intent = Intent(context, RewardActivity::class.java)

        fun getStartIntent(context: Context, deepLink: DeepLink): Intent = getStartIntent(context).apply {
            putExtras(Bundle().apply {
                putString(EXTRA_DEEP_LINK, deepLink.name)
                when (deepLink) {
                    is DeepLink.MissionDetailPage -> {
                        putParcelable(EXTRA_MISSION, deepLink.mission)
                    }
                }
            })
        }
    }
}

interface NavHostActivity {
    fun navigateBackWithResult(result: Bundle)
}

interface NavigationResult {
    fun onNavigationResult(result: Bundle)
}