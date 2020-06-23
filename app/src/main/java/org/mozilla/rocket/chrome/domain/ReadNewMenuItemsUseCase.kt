package org.mozilla.rocket.chrome.domain

import org.mozilla.rocket.chrome.data.MenuRepo
import org.mozilla.rocket.chrome.data.MenuRepo.Companion.MENU_ITEM_VERSION

class ReadNewMenuItemsUseCase(private val menuRepo: MenuRepo) {

    operator fun invoke() {
        menuRepo.saveReadMenuItemVersion(MENU_ITEM_VERSION)
    }
}