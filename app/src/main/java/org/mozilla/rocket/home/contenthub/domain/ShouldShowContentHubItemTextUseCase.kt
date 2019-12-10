package org.mozilla.rocket.home.contenthub.domain

import org.mozilla.focus.utils.FirebaseContract
import org.mozilla.focus.utils.FirebaseHelper

class ShouldShowContentHubItemTextUseCase(private val firebase: FirebaseContract) {

    operator fun invoke(): Boolean = firebase.getRcBoolean(FirebaseHelper.BOOL_CONTENT_HUB_ITEM_TEXT_ENABLED)
}
