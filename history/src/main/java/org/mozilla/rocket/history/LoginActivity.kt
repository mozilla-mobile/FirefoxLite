/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.history

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.storage.sync.SyncAuthInfo
import mozilla.components.concept.storage.SyncError
import mozilla.components.feature.sync.FirefoxSyncFeature
import mozilla.components.feature.sync.SyncStatusObserver
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.FxaException
import mozilla.components.service.fxa.Profile
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.rocket.dynamic.DynamicDeliveryHelper
import kotlin.coroutines.CoroutineContext

class LoginActivity : AppCompatActivity(), LoginFragment.OnLoginCompleteListener, CoroutineScope {

    init {
        Log.d(DynamicDeliveryHelper.TAG, "----LoginActivity loaded----")
    }

    private lateinit var account: FirefoxAccount
    private val scopes: Array<String> = arrayOf("profile", "https://identity.mozilla.com/apps/oldsync")

    private val historyStoreName = "placesHistory"
    private val historyStorage by lazy { PlacesHistoryStorage(applicationContext) }
    private val featureSync by lazy {
        FirefoxSyncFeature(
            mapOf(historyStoreName to historyStorage)
        ) { authInfo ->
            SyncAuthInfo(
                fxaAccessToken = authInfo.fxaAccessToken,
                kid = authInfo.kid,
                syncKey = authInfo.syncKey,
                tokenserverURL = authInfo.tokenServerUrl
            )
        }
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        const val CLIENT_ID = "3c49430b43dfba77"
        const val REDIRECT_URL = "https://accounts.firefox.com/oauth/success/3c49430b43dfba77"
        const val FXA_STATE_PREFS_KEY = "fxaAppState"
        const val FXA_STATE_KEY = "fxaState"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        job = Job()
        account = initAccount()

        // NB: ObserverRegistry takes care of unregistering this observer when appropriate, and
        // cleaning up any internal references to 'observer' and 'owner'.
        featureSync.register(syncObserver, owner = this, autoPause = true)

        if (getAuthenticatedAccount() == null) {

            // do login
            launch {
                val url = account.beginOAuthFlow(scopes, true).await()
                openWebView(url)
            }
            return
        }

        syncNow()
    }

    fun syncNow() {
        // already have account, get data now
        getAuthenticatedAccount()?.let { account ->
            val txtView: TextView = findViewById(R.id.historySyncResult)

            launch {
                val syncResult = CoroutineScope(Dispatchers.IO + job).async {
                    featureSync.sync(account)
                }.await()

                check(historyStoreName in syncResult) { "Expected to synchronize a history store" }

                val historySyncStatus = syncResult[historyStoreName]!!.status
                if (historySyncStatus is SyncError) {
                    txtView.text = getString(R.string.sync_error, historySyncStatus.exception)
                } else {
                    val urls = historyStorage.getVisited()
                    for (url in urls) {
                        val site =
                            BrowsingHistoryManager.prepareSiteForFirstInsert(
                                url,
                                "from sync",
                                System.currentTimeMillis()
                            )
                        BrowsingHistoryManager.getInstance().insert(site, null)
                    }
                    val visitedCount = historyStorage.getVisited().size
                    // visitedCount is passed twice: to get the correct plural form, and then as
                    // an argument for string formatting.
                    txtView.text = "$visitedCount items synced"
                }
            }
        }
    }

    private fun initAccount(): FirefoxAccount {
        getAuthenticatedAccount()?.let {
            launch {
                val profile = it.getProfile(true).await()
                displayProfile(profile)
            }
            return it
        }

        return FirefoxAccount(Config.release(CLIENT_ID, REDIRECT_URL))
    }

    override fun onDestroy() {
        super.onDestroy()
        account.close()
        job.cancel()
    }

    override fun onLoginComplete(code: String, state: String, fragment: LoginFragment) {
        // webview's URL shows it complete's it's login, save the user token
        displayAndPersistProfile(code, state)
        // hide the fragment and the webview
        supportFragmentManager?.popBackStack()
        syncNow()
    }

    private fun getAuthenticatedAccount(): FirefoxAccount? {
        val savedJSON = getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE).getString(FXA_STATE_KEY, "")
        return savedJSON?.let {
            try {
                FirefoxAccount.fromJSONString(it)
            } catch (e: FxaException) {
                null
            }
        }
    }

    private fun openWebView(url: String) {
        supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.container, LoginFragment.create(url, REDIRECT_URL))
            addToBackStack(null)
            commit()
        }
    }

    private fun displayAndPersistProfile(code: String, state: String) {
        launch {
            account.completeOAuthFlow(code, state).await()
            val profile = account.getProfile().await()
            displayProfile(profile)
            account.toJSONString().let {
                getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE)
                    .edit().putString(FXA_STATE_KEY, it).apply()
            }
        }
    }

    private fun displayProfile(profile: Profile) {
        val txtView: TextView = findViewById(R.id.fxaStatusView)
        txtView.text = getString(R.string.signed_in, "${profile.displayName ?: ""} ${profile.email}")
    }

    private val syncObserver = object : SyncStatusObserver {
        override fun onStarted() {
            CoroutineScope(Dispatchers.Main).launch {
                historySyncStatus?.text = getString(R.string.syncing)
            }
        }

        override fun onIdle() {
            CoroutineScope(Dispatchers.Main).launch {
                historySyncStatus?.text = getString(R.string.sync_idle)
                finish()
            }
        }
    }
}
