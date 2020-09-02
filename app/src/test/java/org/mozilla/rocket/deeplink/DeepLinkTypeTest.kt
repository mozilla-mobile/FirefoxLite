package org.mozilla.rocket.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.rocket.deeplink.task.OpenPrivateModeTask
import org.mozilla.rocket.deeplink.task.StartNewsActivityTask
import org.mozilla.rocket.deeplink.task.StartNewsItemActivityTask
import org.mozilla.rocket.deeplink.task.StartRewardActivityTask
import org.mozilla.rocket.deeplink.task.StartSettingsActivityTask
import org.mozilla.rocket.deeplink.task.StartShoppingSearchActivityTask
import java.net.URLEncoder

class DeepLinkTypeTest {

    @Test
    fun `When news home uri is matched, launch news activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/news")

        assertEquals(DeepLinkType.NEWS_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartNewsActivityTask)
    }

    @Test
    fun `When news item uri is matched, launch content tab activity`() {
        val url = "https://www.mozilla.org"
        val feed = "test_feed"
        val source = "test_source"
        val deepLinkType = DeepLinkType.parse("rocket://content/news/item?url=${URLEncoder.encode(url, "utf-8")}&feed=$feed&source=$source")

        assertEquals(DeepLinkType.NEWS_ITEM, deepLinkType)
        val task = deepLinkType.getTaskList()[0]
        assertTrue(task is StartNewsItemActivityTask)
        task as StartNewsItemActivityTask
        assertEquals(task.url, url)
        assertEquals(task.feed, feed)
        assertEquals(task.source, source)
    }

    @Test
    fun `When reward home uri is matched, launch reward activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/reward")

        assertEquals(DeepLinkType.REWARD_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartRewardActivityTask)
    }

    @Test
    fun `When shopping search home uri is matched, launch shopping search activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/shopping-search")

        assertEquals(DeepLinkType.SHOPPING_SEARCH_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartShoppingSearchActivityTask)
    }

    @Test
    fun `When private mode uri is matched, launch private mode activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://private-mode")

        assertEquals(DeepLinkType.PRIVATE_MODE, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is OpenPrivateModeTask)
    }

    @Test
    fun `When set default browser command uri is matched, show set default browser dialog`() {
        val deepLinkType = DeepLinkType.parse("rocket://command?command=${DeepLinkConstants.COMMAND_SET_DEFAULT_BROWSER}")

        assertEquals(DeepLinkType.COMMAND_SET_DEFAULT_BROWSER, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartSettingsActivityTask)
    }
}