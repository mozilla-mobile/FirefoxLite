package org.mozilla.rocket.chrome.domain

import org.mozilla.rocket.chrome.data.MenuRepo
import org.mozilla.rocket.chrome.data.MenuRepo.Companion.MENU_ITEM_VERSION

class ShouldShowNewMenuItemHintUseCase(private val menuRepo: MenuRepo) {

    operator fun invoke(): Boolean = menuRepo.getReadMenuItemVersion() < MENU_ITEM_VERSION
}