package org.mozilla.rocket.content.ecommerce.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.ecommerce.data.ShoppingRemoteDataSource
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepository
import org.mozilla.rocket.content.ecommerce.domain.GetCouponsUseCase
import org.mozilla.rocket.content.ecommerce.domain.GetDealsUseCase
import org.mozilla.rocket.content.ecommerce.domain.GetShoppingTabItemsUseCase
import org.mozilla.rocket.content.ecommerce.domain.GetVouchersUseCase
import org.mozilla.rocket.content.ecommerce.ui.CouponViewModel
import org.mozilla.rocket.content.ecommerce.ui.DealViewModel
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel
import org.mozilla.rocket.content.ecommerce.ui.VoucherViewModel
import javax.inject.Singleton

@Module
object ShoppingModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingRemoteDataSource(): ShoppingRemoteDataSource =
        ShoppingRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingRepository(shoppingDataSource: ShoppingRemoteDataSource): ShoppingRepository =
        ShoppingRepository(shoppingDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetDealsUseCase(repo: ShoppingRepository): GetDealsUseCase =
        GetDealsUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCouponsUseCase(repo: ShoppingRepository): GetCouponsUseCase =
        GetCouponsUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetVouchersUseCase(repo: ShoppingRepository): GetVouchersUseCase =
        GetVouchersUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetShoppingTabItemsUseCase(repo: ShoppingRepository): GetShoppingTabItemsUseCase =
        GetShoppingTabItemsUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingViewModel(getShoppingTabItemsUseCase: GetShoppingTabItemsUseCase): ShoppingViewModel =
        ShoppingViewModel(getShoppingTabItemsUseCase)

    @JvmStatic
    @Provides
    fun provideDealViewModel(getDealsUseCase: GetDealsUseCase): DealViewModel =
        DealViewModel(getDealsUseCase)

    @JvmStatic
    @Provides
    fun provideCouponViewModel(getCouponsUseCase: GetCouponsUseCase): CouponViewModel =
        CouponViewModel(getCouponsUseCase)

    @JvmStatic
    @Provides
    fun provideVoucherViewModel(getVouchersUseCase: GetVouchersUseCase): VoucherViewModel =
        VoucherViewModel(getVouchersUseCase)
}