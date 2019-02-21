package org.mozilla.rocket.urlinput

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// QuickSearch uses android.net.Uri so Robolectric is required.
@RunWith(RobolectricTestRunner::class)
class QuickSearchTest {

    @Test
    fun `Instagram will remove space in search term`() {
        val output = QuickSearch(
                "Instagram", "", "https://www.instagram.com/explore/tags/%s/",
                "", "", "", removeSpace = true, patternEncode = false
        ).generateLink("Privacy Mode")
        assertEquals("https://www.instagram.com/explore/tags/PrivacyMode/", output)
    }

    @Test
    fun `Flipkart won't remove space`() {
        val output = QuickSearch(
                "Flipkart", "", "https://www.flipkart.com/search?q=%s",
                "", "", ""
        ).generateLink("Privacy Mode")
        assertEquals("https://www.flipkart.com/search?q=Privacy%20Mode", output)
    }

    @Test
    fun `Bukalapak needs special handle`() {
        val output = QuickSearch(
                "Buakalapak",
                "",
                "https://m.bukalapak.com/products?keywords=%s",
                "",
                "https://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=",
                "%26{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id" +
                        "}%26utm_source%3Dhasoffers-{affiliate_id}%26utm_medium" +
                        "%3Daffiliate",
                removeSpace = false,
                patternEncode = true
        ).generateLink("Privacy Mode")
        assertEquals(
                "https://bukalapak.go2cloud" +
                        ".org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fm.bukalapak" +
                        ".com%2Fproducts%3Fkeywords%3DPrivacy%20Mode%26{offer_id" +
                        "}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id" +
                        "}%26utm_source%3Dhasoffers-{affiliate_id}%26utm_medium%3Daffiliate",
                output
        )
    }
}