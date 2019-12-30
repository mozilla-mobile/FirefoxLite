package org.mozilla.rocket.home.logoman.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo

class LastReadLogoManNotificationUseCase(
    private val logoManNotificationRepo: LogoManNotificationRepo
) {

    operator fun invoke(): LiveData<String> = logoManNotificationRepo.getLastReadNotificationId()
}