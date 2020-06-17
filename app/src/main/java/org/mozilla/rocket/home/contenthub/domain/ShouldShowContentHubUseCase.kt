package org.mozilla.rocket.home.contenthub.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo

class ShouldShowContentHubUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(): LiveData<Boolean> = contentHubRepo.isContentHubEnabled()
}
