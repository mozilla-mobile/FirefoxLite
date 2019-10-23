package org.mozilla.rocket.fxa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.msrp.domain.BindFxAccountUseCase
import org.mozilla.rocket.msrp.domain.GetUserIdUseCase
import org.mozilla.rocket.msrp.domain.IsFxAccountUseCase
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.widget.FxToast
import javax.inject.Inject

class ProfileActivity : FragmentActivity(), TabsSessionProvider.SessionHost, FxLoginFragment.OnLoginCompleteListener {

    @Inject
    lateinit var isFxAccountUseCase: IsFxAccountUseCase
    @Inject
    lateinit var getUserIdUseCase: GetUserIdUseCase
    @Inject
    lateinit var bindFxAccountUseCase: BindFxAccountUseCase

    private lateinit var tabViewProvider: TabViewProvider
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        tabViewProvider = PrivateTabViewProvider(this)
        sessionManager = SessionManager(tabViewProvider)

        setContentView(R.layout.activity_profile)

        requireLoginIfNeeded()
    }

    private fun requireLoginIfNeeded() {
        if (!isFxAccountUseCase()) {
            navigateToFxLoginPage(getUserIdUseCase())
        }
    }

    private fun navigateToFxLoginPage(uid: String) {
        findNavController(R.id.nav_host_fragment)
                .navigate(FxAccountFragmentDirections.actionFxAccountDestToFxLogin2Dest(0, uid))
    }

    override fun onLoginSuccess(requestCode: Int, jwt: String, isDisabled: Boolean, times: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            TelemetryWrapper.accountSignIn()
            bindFxAccountUseCase(jwt)
            when {
                isDisabled -> {
                    DialogUtils.showAccountDisabledDialog(this@ProfileActivity) {
                        finish()
                    }
                    return@launch
                }
                times == 1 -> DialogUtils.showLoginMultipleTimesWarningDialog(this@ProfileActivity)
                times == 2 -> DialogUtils.showLoginMultipleTimesFinalWarningDialog(this@ProfileActivity)
            }
            navigateToFxAccountPage()
        }
    }

    override fun onLoginFailure() {
        FxToast.show(applicationContext, getString(R.string.msrp_reward_challenge_error))
    }

    private fun navigateToFxAccountPage() {
        findNavController(R.id.nav_host_fragment)
                .navigate(FxLoginFragment2Directions.actionFxLogin2DestToFxAccountDest())
    }

    override fun getSessionManager(): SessionManager = sessionManager

    companion object {
        fun getStartIntent(context: Context): Intent =
                Intent(context, ProfileActivity::class.java)
    }
}