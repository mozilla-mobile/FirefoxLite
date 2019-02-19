package org.mozilla.rocket.geckopower

import android.os.Bundle
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class GeckoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val view = findViewById<GeckoView>(R.id.geckoview)
        val view = GeckoView(this)
        val session = GeckoSession()
        val runtime = GeckoRuntime.create(this)

        session.open(runtime)
        view.session = session
        session.loadUri("about:buildconfig")
        setContentView(view)
    }

    override fun applyLocale() {
    }
}