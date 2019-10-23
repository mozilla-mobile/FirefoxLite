package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

class NewsActivity : AppCompatActivity() {

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var telemetryViewModel: VerticalTelemetryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NewsTabFragment.newInstance())
                .commitNow()
        }
    }

    override fun onResume() {
        super.onResume()
        telemetryViewModel.onSessionStarted(TelemetryWrapper.Extra_Value.LIFESTYLE)
    }

    override fun onPause() {
        super.onPause()
        telemetryViewModel.onSessionEnded(TelemetryWrapper.Extra_Value.LIFESTYLE)
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, NewsActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
