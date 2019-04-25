package org.mozilla.rocket.content

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mozilla.rocket.content.news.NewsPresenter
import org.mozilla.rocket.content.news.NewsViewContract
import org.mozilla.rocket.content.news.NewsViewModel
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NewsPresenterTest {

    lateinit var newsPresenter: NewsPresenter

    lateinit var viewContract: NewsViewContract
    lateinit var newsViewModel: NewsViewModel

    @Before
    fun warmUp() {
        viewContract = mock(NewsViewContract::class.java)
        newsViewModel = mock(NewsViewModel::class.java)
        newsPresenter = NewsPresenter(viewContract)
        newsPresenter.newsViewModel = newsViewModel
    }

    @Test
    fun `Avoid rapidly loading more news items`() {
        newsPresenter.loadMore()
        newsPresenter.loadMore()
        Mockito.verify(newsViewModel, times(1)).loadMore()
    }

    @Test
    fun `when Repository is not set, checkNewsRepositoryReset() should force load more news items`() {
        newsPresenter.checkNewsRepositoryReset(ApplicationProvider.getApplicationContext<Application>())
        Mockito.verify(newsViewModel, times(1)).loadMore()
    }
}