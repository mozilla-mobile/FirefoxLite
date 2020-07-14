package org.mozilla.rocket.home.domain

import org.mozilla.rocket.home.data.ContentPrefRepo

class SetContentPrefUseCase(private val contentPrefRepo: ContentPrefRepo) {

    operator fun invoke(contentPref: ContentPrefRepo.ContentPref) {
        contentPrefRepo.setContentPref(contentPref)
    }
}