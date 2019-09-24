package org.mozilla.rocket.msrp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.fxa.FxLoginFragment

class RewardActivity : AppCompatActivity(), FxLoginFragment.OnLoginCompleteListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward)
    }

    override fun onLoginComplete(jwt: String, fragmentFx: FxLoginFragment) {
        // TODO: Add the log to profile the slowness of the login
        FirebaseHelper.signInWithCustomToken(jwt, this, { fxUid, oldFbUid ->
            Log.d(LOG_TAG, "onLoginComplete success oldFbUid [$oldFbUid] is now matches to Fx User [$fxUid]")
        }, {
            Log.d(LOG_TAG, "onLoginComplete fail ===$it")
        })

        supportFragmentManager.popBackStack()

        // TODO: Evan
//        screenNavigator.addRedeem()
    }

    companion object {
        private const val LOG_TAG = "RewardActivity"
        fun getStartIntent(context: Context): Intent = Intent(context, RewardActivity::class.java)
    }
}