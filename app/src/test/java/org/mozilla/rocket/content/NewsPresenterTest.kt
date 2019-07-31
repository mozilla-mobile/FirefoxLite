package org.mozilla.rocket.content

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.ui.NewsPresenter
import org.mozilla.rocket.content.news.ui.NewsViewContract
import org.mozilla.rocket.content.news.ui.NewsViewModel
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
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
        newsPresenter = NewsPresenter(viewContract, newsViewModel)
    }

    @Test
    fun `Avoid rapidly loading more news items`() {
        newsPresenter.loadMore()
        newsPresenter.loadMore()
        `when`(viewContract.getCategory())?.thenReturn("")
        Mockito.verify(newsViewModel, times(1)).loadMore(anyOrNull())
    }

    @Test
    fun `test contract needed for setupViewModel `() {
        newsPresenter.setupNewsViewModel(
            mock(Repository::class.java),
            mock(NewsSettingsRepository::class.java)
        )
        Mockito.verify(viewContract, times(1)).getCategory()
        Mockito.verify(viewContract, times(1)).getLanguage()
        Mockito.verify(viewContract, times(1)).getViewLifecycleOwner()
        Mockito.verify(viewContract, times(1)).updateSourcePriority()
        Mockito.verify(newsViewModel, times(1)).loadMore(anyOrNull())
    }
}

/**
 * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when
 * null is returned.
 */
fun <T> anyOrNull(): T = Mockito.any<T>()