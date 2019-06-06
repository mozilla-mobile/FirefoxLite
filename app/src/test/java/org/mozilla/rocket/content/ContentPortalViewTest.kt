package org.mozilla.rocket.content

import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.focus.R
import org.mozilla.rocket.content.portal.ContentFeature
import org.mozilla.rocket.content.portal.ContentPortalView
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ContentPortalViewTest {

    private lateinit var contentPortalView: ContentPortalView

    private lateinit var activity: FragmentActivity

    @Before
    @Throws(Exception::class)
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        contentPortalView =
            LayoutInflater.from(activity).inflate(R.layout.content_portal, null) as ContentPortalView
        activity.setContentView(contentPortalView)
    }

    @Test
    fun `Swipe up or tap the arrow to show content portal panel`() {

        assert(contentPortalView.visibility == View.GONE)

        contentPortalView.show(false)
        assert(contentPortalView.visibility == View.VISIBLE)
        assert(ContentPortalViewState.isOpened())

        contentPortalView.hide()
        assert(contentPortalView.visibility == View.GONE)
        assert(!ContentPortalViewState.isOpened())
    }

    @Test
    fun `when there's News in RemoteConfig, we show NewsTabFragment`() {
        val mockFeature = mock(ContentFeature::class.java)
        contentPortalView.contentFeature = mockFeature
        `when`(mockFeature.hasNews()).thenReturn(true)
        contentPortalView.onAttachedToWindow()
        assert(activity.supportFragmentManager.findFragmentByTag(ContentPortalView.TAG_NEWS_FRAGMENT) != null)
    }

    @Test
    fun `when there's no News in RemoteConfig, we show EcTabFragment`() {
        contentPortalView.onAttachedToWindow()
        assert(activity.supportFragmentManager.findFragmentByTag(ContentPortalView.TAG_CONTENT_FRAGMENT) != null)
    }

    @Test
    fun `Remember to re-open Content Portal once opend`() {
        ContentPortalViewState.lastOpened()
        contentPortalView.onResume()
        assert(contentPortalView.visibility == View.VISIBLE)

        ContentPortalViewState.reset()
        contentPortalView.onResume()
        assert(contentPortalView.visibility == View.GONE)
    }
}