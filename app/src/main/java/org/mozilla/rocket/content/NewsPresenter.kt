package org.mozilla.rocket.content

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.annotation.VisibleForTesting
import android.support.v4.app.FragmentActivity
import org.mozilla.focus.utils.Settings
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.widget.NewsSourcePreference.PREF_INT_NEWS_PRIORITY
import org.mozilla.threadutils.ThreadUtils

interface NewsViewContract {
    fun getViewLifecycleOwner(): LifecycleOwner
    fun setData(items: List<NewsItem>?)
}

class NewsPresenter(private val newsViewContract: NewsViewContract) : ContentPortalView.NewsListListener {

    @VisibleForTesting
    var contentViewModel: ContentViewModel? = null

    companion object {
        private val LOADMORE_THRESHOLD = 3000L
    }
    private var isLoading = false

    fun setupContentViewModel(fragmentActivity: FragmentActivity?) {
        if (fragmentActivity == null) {
            return
        }
        contentViewModel = ViewModelProviders.of(fragmentActivity).get(ContentViewModel::class.java)
        val repository = ContentRepository.getInstance(fragmentActivity)
        repository.setOnDataChangedListener(contentViewModel)
        contentViewModel?.repository = repository
        contentViewModel?.items?.observe(newsViewContract.getViewLifecycleOwner(),
                Observer { items ->
                    newsViewContract.setData(items)
                    isLoading = false
                })
        // creating a repository will also create a new subscription.
        // we deliberately create a new subscription again to load data aggressively.
        contentViewModel?.loadMore()
    }

    override fun loadMore() {
        if (!isLoading) {
            contentViewModel?.loadMore()
            isLoading = true
            ThreadUtils.postToMainThreadDelayed({ isLoading = false }, LOADMORE_THRESHOLD)
        }
    }

    override fun onShow(context: Context) {
        updateSourcePriority(context)
        checkNewsRepositoryReset(context)
    }

    fun checkNewsRepositoryReset(context: Context) {
        // News Repository is reset and empty when the user changes the news source.
        // We create a new Repository and inject to contentViewModel here.
        // TODO: similar code happens in setupContentViewModel(), need to refine them
        if (ContentRepository.isEmpty() && contentViewModel != null) {
            val repository = ContentRepository.getInstance(context)
            repository.setOnDataChangedListener(contentViewModel)
            contentViewModel?.repository = repository
            contentViewModel?.items?.value = null
            contentViewModel?.loadMore()
        }
    }

    private fun updateSourcePriority(context: Context) {
        // the user had seen the news. Treat it as an user selection so no on can change it
        Settings.getInstance(context).setPriority(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_USER)
    }
}