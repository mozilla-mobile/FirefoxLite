package org.mozilla.rocket.shopping.search.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mozilla.rocket.util.LiveDataTestUtil

class ShoppingSearchRepositoryTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var remoteDataSource: ShoppingSearchDataSource
    private lateinit var localDataSource: ShoppingSearchDataSource
    private lateinit var shoppingSearchRepository: ShoppingSearchRepository

    @Before
    fun setUp() {
        remoteDataSource = Mockito.mock(ShoppingSearchDataSource::class.java)
        localDataSource = Mockito.mock(ShoppingSearchDataSource::class.java)
    }

    @Test
    fun `When it's the first time retrieved shopping site data from remote, it should return the exact same list from remote`() {
        Mockito.`when`(remoteDataSource.getShoppingSites())
            .thenReturn(listOf(
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = true)
            ))
        Mockito.`when`(localDataSource.getShoppingSites())
            .thenReturn(emptyList())

        shoppingSearchRepository = ShoppingSearchRepository(remoteDataSource, localDataSource)

        Assert.assertEquals(
            listOf(
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = true)
            ),
            LiveDataTestUtil.getValue(shoppingSearchRepository.getShoppingSitesData()))
    }

    @Test
    fun `When there is new shopping site data from remote, it should return local result plus new item`() {
        Mockito.`when`(remoteDataSource.getShoppingSites())
            .thenReturn(listOf(
                ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", true, isEnabled = true),
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = true),
                ShoppingSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=", "tokopedia.com", true, isEnabled = true)
            ))
        Mockito.`when`(localDataSource.getShoppingSites())
            .thenReturn(listOf(
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = true),
                ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", true, isEnabled = false)
            ))

        shoppingSearchRepository = ShoppingSearchRepository(remoteDataSource, localDataSource)

        Assert.assertEquals(
            listOf(
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = true),
                ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", true, isEnabled = false),
                ShoppingSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=", "tokopedia.com", true, isEnabled = true)
            ),
            LiveDataTestUtil.getValue(shoppingSearchRepository.getShoppingSitesData()))
    }

    @Test
    fun `When there is item removed from remote, it should return local result without removed item`() {
        Mockito.`when`(remoteDataSource.getShoppingSites())
            .thenReturn(listOf(
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = true),
                ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", true, isEnabled = true)
            ))
        Mockito.`when`(localDataSource.getShoppingSites())
            .thenReturn(listOf(
                ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", true, isEnabled = true),
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = false),
                ShoppingSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=", "tokopedia.com", true, isEnabled = true)
            ))

        shoppingSearchRepository = ShoppingSearchRepository(remoteDataSource, localDataSource)

        Assert.assertEquals(
            listOf(
                ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", true, isEnabled = true),
                ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", true, isEnabled = false)
            ),
            LiveDataTestUtil.getValue(shoppingSearchRepository.getShoppingSitesData()))
    }
}