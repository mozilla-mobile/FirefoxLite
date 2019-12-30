package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.parcel.Parcelize
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.util.sha256
import javax.inject.Inject

class NewsActivity : AppCompatActivity() {

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    @Inject
    lateinit var newsOnboardingViewModelCreator: Lazy<NewsOnboardingViewModel>

    private lateinit var newsOnboardingViewModel: NewsOnboardingViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        newsOnboardingViewModel = getViewModel(newsOnboardingViewModelCreator)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NewsTabFragment.newInstance())
                .commitNow()

            intent.extras?.let {
                parseDeepLink(it)
            }
        }

        newsOnboardingViewModel.languageOnboardingDone.observe(this, Observer {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, NewsTabFragment.newInstance())
                    .commitNow()
        })
    }

    private fun parseDeepLink(bundle: Bundle): Boolean {
        when (bundle.getString(EXTRA_DEEP_LINK) ?: return false) {
            DEEP_LINK_NEWS_ITEM_PAGE -> openNewsItemPageFromDeepLink(bundle.getParcelable(EXTRA_NEWS_ITEM_DATA)!!)
        }

        return true
    }

    private fun openNewsItemPageFromDeepLink(newsItemData: DeepLink.NewsItemPage.Data) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.LIFESTYLE,
            newsItemData.feed,
            newsItemData.source,
            "", // no need when triggered by deep link
            newsItemData.url.sha256(),
            "", // no need when triggered by deep link
            System.currentTimeMillis()
        )
        startActivity(ContentTabActivity.getStartIntent(this, newsItemData.url, telemetryData, enableTurboMode = false))
    }

    override fun onResume() {
        super.onResume()
        telemetryViewModel.onSessionStarted(TelemetryWrapper.Extra_Value.LIFESTYLE)
    }

    override fun onPause() {
        super.onPause()
        telemetryViewModel.onSessionEnded()
    }

    sealed class DeepLink(val name: String) {
        data class NewsItemPage(val data: Data) : DeepLink(DEEP_LINK_NEWS_ITEM_PAGE) {
            @Parcelize
            data class Data(val url: String, val feed: String, val source: String) : Parcelable
        }
    }

    companion object {
        private const val EXTRA_DEEP_LINK = "extra_deep_link"
        private const val EXTRA_NEWS_ITEM_DATA = "extra_news_item_data"
        private const val DEEP_LINK_NEWS_ITEM_PAGE = "deep_link_news_item_page"

        fun getStartIntent(context: Context) =
            Intent(context, NewsActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        fun getStartIntent(context: Context, deepLink: DeepLink) = getStartIntent(context).apply {
            putExtras(Bundle().apply {
                putString(EXTRA_DEEP_LINK, deepLink.name)
                when (deepLink) {
                    is DeepLink.NewsItemPage -> {
                        putParcelable(EXTRA_NEWS_ITEM_DATA, deepLink.data)
                    }
                }
            })
        }
    }
}
