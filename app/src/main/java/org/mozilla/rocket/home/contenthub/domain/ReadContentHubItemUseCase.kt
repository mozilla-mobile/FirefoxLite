package org.mozilla.rocket.home.contenthub.domain

import org.mozilla.rocket.home.contenthub.data.ContentHubRepo

class ReadContentHubItemUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(type: Int) {
        contentHubRepo.addReadType(type)
    }
}
