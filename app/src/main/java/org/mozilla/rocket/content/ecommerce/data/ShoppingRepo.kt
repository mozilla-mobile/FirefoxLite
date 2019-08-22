package org.mozilla.rocket.content.ecommerce.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.adapter.CouponRunway
import org.mozilla.rocket.content.ecommerce.adapter.CouponRunwayItem
import kotlin.random.Random

class ShoppingRepo {

    suspend fun getCoupons(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<DelegateAdapter.UiModel>()
            list.add(CouponRunway(listOf(
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem()
            )))
            list.addAll(AppConfigWrapper.getEcommerceCoupons())
            return@withContext list
        }
    }

    suspend fun getShoppingLinks(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            AppConfigWrapper.getEcommerceShoppingLinks()
        }
    }

    // TODO: remove test function
    private fun getPlaceholderImageUrl(w: Int, h: Int): String =
            "https://placeimg.com/$w/$h/animals?whatever=${Random.nextInt(0, 10)}"

    // TODO: remove test function
    private fun generateFakeRunwayItem(): CouponRunwayItem =
            getPlaceholderImageUrl(400, 200).run { CouponRunwayItem(this, this, this) }
}