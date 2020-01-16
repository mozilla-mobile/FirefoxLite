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
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.extension.isLaunchedFromHistory
import org.mozilla.rocket.util.sha256
import javax.inject.Inject

class NewsActivity : AppCompatActivity() {

    @Inject
    lateinit var newsPageStateViewModelCreator: Lazy<NewsPageStateViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var newsPageStateViewModel: NewsPageStateViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        newsPageStateViewModel = getViewModel(newsPageStateViewModelCreator)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)

        newsPageStateViewModel.showContent.observe(this, Observer { content ->
            val fragment = when (content) {
                is NewsPageStateViewModel.Page.PersonalizationOnboarding -> PersonalizedNewsOnboardingFragment.newInstance()
                is NewsPageStateViewModel.Page.LanguageSetting -> NewsLanguageSettingFragment.newInstance()
                is NewsPageStateViewModel.Page.NewsContent -> NewsTabFragment.newInstance()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
        })

        if (savedInstanceState == null) {
            newsPageStateViewModel.checkPageToShow()

            intent.extras?.let {
                if (!isLaunchedFromHistory()) {
                    parseDeepLink(it)
                }
            }
        }
    }

    private fun parseDeepLink(bundle: Bundle): Boolean {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
            return false
        }
        when (bundle.getString(EXTRA_DEEP_LINK) ?: return false) {
            DEEP_LINK_NEWS_ITEM_PAGE -> {
                openNewsItemPageFromDeepLink(bundle.getParcelable(EXTRA_NEWS_ITEM_DATA)!!)
            }
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

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        if (supportFragmentManager.fragments.size > 0) {
            val fragment = supportFragmentManager.fragments[0]
            if (fragment is BackKeyHandleable) {
                val handled = fragment.onBackPressed()
                if (handled) {
                    return
                }
            }
        }

        super.onBackPressed()
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
