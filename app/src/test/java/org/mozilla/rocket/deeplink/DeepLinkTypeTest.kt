package org.mozilla.rocket.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.rocket.deeplink.task.StartGameActivityTask
import org.mozilla.rocket.deeplink.task.StartNewsActivityTask
import org.mozilla.rocket.deeplink.task.StartRewardActivityTask
import org.mozilla.rocket.deeplink.task.StartShoppingActivityTask
import org.mozilla.rocket.deeplink.task.StartTravelActivityTask

class DeepLinkTypeTest {

    @Test
    fun `When game home uri is matched, launch game activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/game")

        assertEquals(DeepLinkType.GAME_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartGameActivityTask)
    }

    @Test
    fun `When news home uri is matched, launch news activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/news")

        assertEquals(DeepLinkType.NEWS_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartNewsActivityTask)
    }

    @Test
    fun `When shopping home uri is matched, launch shopping activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/shopping")

        assertEquals(DeepLinkType.SHOPPING_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartShoppingActivityTask)
    }

    @Test
    fun `When travel home uri is matched, launch travel activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/travel")

        assertEquals(DeepLinkType.TRAVEL_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartTravelActivityTask)
    }

    @Test
    fun `When reward home uri is matched, launch reward activity`() {
        val deepLinkType = DeepLinkType.parse("rocket://content/reward")

        assertEquals(DeepLinkType.REWARD_HOME, deepLinkType)
        assertTrue(deepLinkType.getTaskList()[0] is StartRewardActivityTask)
    }
}