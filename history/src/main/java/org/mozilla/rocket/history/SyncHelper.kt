package org.mozilla.rocket.history

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.util.Log
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
import kotlin.coroutines.CoroutineContext

class SyncHelper(context: Context) : CoroutineScope {

    val applicationContext = context.applicationContext
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

    fun init(lifecycleOwner: LifecycleOwner) {

        // NB: ObserverRegistry takes care of unregistering this observer when appropriate, and
        // cleaning up any internal references to 'observer' and 'owner'.
        featureSync.register(syncObserver, owner = lifecycleOwner, autoPause = true)
    }

    fun logout(context: Context) {
        context.getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE).edit().putString(FXA_STATE_KEY, "")
            .apply()
    }

    fun beginOAuthFlow(displayProfile: (Profile) -> Unit, context: Context) {

        job = Job()
        account = initAccount(displayProfile, context)

        launch {
            val url = account.beginOAuthFlow(scopes, true).await()
            openWebView(url)
        }

        fun sync(
            error: (String) -> Unit,
            success: (List<String>) -> Unit,
            context: Context
        ) {
            getAuthenticatedAccount(context)?.let { account ->

                launch {
                    val syncResult = CoroutineScope(Dispatchers.IO + job).async {
                        featureSync.sync(account)
                    }.await()

                    check(historyStoreName in syncResult) { "Expected to synchronize a history store" }
                    error("")
                    val historySyncStatus = syncResult[historyStoreName]!!.status
                    if (historySyncStatus is SyncError) {
                        error(historySyncStatus.exception)
                    } else {
                        success(historyStorage.getVisited())
                    }
                }
            }
        }
    }

    private fun initAccount(
        displayProfile: (Profile) -> Unit,
        context: Context
    ): FirefoxAccount {
        getAuthenticatedAccount(context)?.let {
            launch {
                val profile = it.getProfile(true).await()
                displayProfile(profile)
            }
            return it
        }

        return FirefoxAccount(Config.release(CLIENT_ID, REDIRECT_URL))
    }

    fun onDestroy() {
        account.close()
        job.cancel()
    }

    private fun getAuthenticatedAccount(context: Context): FirefoxAccount? {
        val savedJSON =
            context.getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE).getString(FXA_STATE_KEY, "")
        return savedJSON?.let {
            try {
                FirefoxAccount.fromJSONString(it)
            } catch (e: FxaException) {
                null
            }
        }
    }

    private fun openWebView(url: String) {
    }

    private fun displayAndPersistProfile(context: Context, code: String, state: String) {
        launch {
            account.completeOAuthFlow(code, state).await()
//            val profile = account.getProfile().await()
//            displayProfile(context, profile)
            account.toJSONString().let {
                context.getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE)
                    .edit().putString(FXA_STATE_KEY, it).apply()
            }
        }
    }

    private val syncObserver = object : SyncStatusObserver {
        override fun onStarted() {
            CoroutineScope(Dispatchers.Main).launch {
                Log.e("aaaa", "symcing")
            }
        }

        override fun onIdle() {
            CoroutineScope(Dispatchers.Main).launch {
                Log.e("aaaa", "idle")
            }
        }
    }
}