package org.mozilla.rocket.privately

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.utils.ThreadUtils
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.TabUtil


class PrivateModeActivity : LocaleAwareAppCompatActivity(), TabsSessionProvider.SessionHost {

    private var backBtn: LottieAnimationView? = null
    private var logoMan: LottieAnimationView? = null
    private var webViewSlot: FrameLayout? = null
    private var tabsSession: TabsSession? = null
    private var privateMode: PrivateMode? = null

    private var adding = false


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_browsing)

        webViewSlot = findViewById(R.id.webview_private_slot)

        backBtn = findViewById(R.id.btn_tab_tray_private)
        backBtn?.setOnClickListener {
            pushToBack()
        }

        logoMan = findViewById(R.id.logo_man_private)
        logoMan?.setOnClickListener {
            if (adding) {
                fakeNewTab()
            } else {
                fakeCloseTab()
            }
            adding = !adding

        }


        makeStatusBarTransparent()

    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        animatePrivateHome()
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        pushToBack()
    }

    private fun pushToBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        overridePendingTransition(0, R.anim.pb_exit)
    }

    override fun applyLocale() {

    }

    private fun makeStatusBarTransparent() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
    }

    private fun animatePrivateHome() {
        backBtn?.playAnimation()
        logoMan?.playAnimation()
    }

    override fun getTabsSession(): TabsSession? {
        if (tabsSession == null) {
            val provider = MainTabViewProvider(this)
            tabsSession = TabsSession(provider)
        }
        return tabsSession
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {

    }

    // a TabViewProvider and it should only be used in this activity
    private class MainTabViewProvider internal constructor(private val activity: Activity) : TabViewProvider {

        override fun create(): TabView {
            // FIXME: we should avoid casting here.
            // TabView and View is totally different, we know WebViewProvider returns a TabView for now,
            // but there is no promise about this.
            return WebViewProvider.create(this.activity, null) as TabView
        }
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val lb = service as PrivateSessionNotificationService.LocalBinder
            privateMode = lb.privateMode


        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    private fun bindService() {
        val intent = Intent(this, PrivateSessionNotificationService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        unbindService(connection)
    }

    private fun fakeCloseTab() {
        val tab = tabsSession?.focusTab
        tabsSession?.dropTab(tab?.id!!)
        privateMode?.unregister(tab)
    }

    private fun fakeNewTab() {
        tabsSession = TabsSessionProvider.getOrThrow(this)
        tabsSession?.addTab("https://www.google.com", TabUtil.argument(null, false, true))
        webViewSlot?.addView(getTabsSession()?.focusTab?.tabView?.view)
        ThreadUtils.postToBackgroundThread {
            privateMode?.register(tabsSession?.focusTab, Footprint.generate(this@PrivateModeActivity))
        }

    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
        unbindService()
    }

}
