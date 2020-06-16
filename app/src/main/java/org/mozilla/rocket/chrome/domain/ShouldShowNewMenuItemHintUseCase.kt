package org.mozilla.rocket.chrome.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.chrome.data.MenuRepo
import org.mozilla.rocket.chrome.data.MenuRepo.Companion.MENU_ITEM_VERSION
import org.mozilla.rocket.extension.map

class ShouldShowNewMenuItemHintUseCase(private val menuRepo: MenuRepo) {

    operator fun invoke(): LiveData<Boolean> = menuRepo.getReadMenuItemVersionLiveData()
            .map { it < MENU_ITEM_VERSION }
}