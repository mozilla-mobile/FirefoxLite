package org.mozilla.rocket.content.news.domain

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class DailyHuntUtilsTest {

    @Test
    fun testGenerateSignatureBase() {
        val secret = "=="
        val apiKey = "g="
        val params = mapOf("partner" to "moz",
                "langCode" to "en",
                "ts" to "1575970648758",
                "puid" to "123456789lkjhgfds",
                "pfm" to "16")
        val generateSignatureBase = DailyHuntUtils.generateSignatureBase(params)

        // expect g9Rkd1M8pbxzH445fAHPWBL6de0
        println("generateSignatureBase=$generateSignatureBase")

        val calculateRFC2104HMAC = DailyHuntUtils.calculateRFC2104HMAC(generateSignatureBase + "GET", secret)
        println("calculateRFC2104HMAC=$calculateRFC2104HMAC")

        println("-----command------\ncurl -X GET 'http://qa-news.newshunt.com/api/v2/syndication/channels?$generateSignatureBase'  -H 'Authorization: key=$apiKey' -H 'Signature: $calculateRFC2104HMAC' ")
    }
}