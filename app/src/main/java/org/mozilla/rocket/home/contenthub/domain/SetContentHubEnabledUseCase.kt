package org.mozilla.rocket.home.contenthub.domain

import org.mozilla.rocket.home.contenthub.data.ContentHubRepo

class SetContentHubEnabledUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(enabled: Boolean) {
        contentHubRepo.setContentHubEnabled(enabled)
    }
}
