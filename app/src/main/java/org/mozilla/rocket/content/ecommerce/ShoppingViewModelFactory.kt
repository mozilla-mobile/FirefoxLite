package org.mozilla.rocket.content.ecommerce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepo

class ShoppingViewModelFactory private constructor(
    private val shoppingRepo: ShoppingRepo
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            return ShoppingViewModel(shoppingRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {
        @JvmStatic
        val INSTANCE: ShoppingViewModelFactory by lazy { ShoppingViewModelFactory(ShoppingRepo()) }
    }
}