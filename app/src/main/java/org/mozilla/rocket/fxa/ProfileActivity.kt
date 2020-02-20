package org.mozilla.rocket.fxa

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.fxa.FxLoginFragment.Companion.STATUS_CODE_FINAL_WARNING
import org.mozilla.rocket.fxa.FxLoginFragment.Companion.STATUS_CODE_WARNING
import org.mozilla.rocket.msrp.domain.BindFxAccountUseCase
import org.mozilla.rocket.msrp.domain.GetUserIdUseCase
import org.mozilla.rocket.msrp.domain.IsFxAccountUseCase
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider
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

    override fun onLoginSuccess(requestCode: Int, jwt: String, isDisabled: Boolean, statusCode: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            TelemetryWrapper.accountSignIn()
            bindFxAccountUseCase(jwt)
            when {
                isDisabled -> {
                    DialogUtils.showAccountDisabledDialog(this@ProfileActivity, DialogInterface.OnDismissListener {
                        finish()
                    })
                    return@launch
                }
                statusCode == STATUS_CODE_WARNING -> DialogUtils.showLoginMultipleTimesWarningDialog(this@ProfileActivity)
                statusCode == STATUS_CODE_FINAL_WARNING -> DialogUtils.showLoginMultipleTimesFinalWarningDialog(this@ProfileActivity)
            }
            navigateToFxAccountPage()
        }
    }

    override fun onLoginFailure() {
        Toast.makeText(applicationContext, R.string.msrp_reward_challenge_error, Toast.LENGTH_LONG).show()
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