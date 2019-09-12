package org.mozilla.rocket.home.logoman.domain

import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.data.Notification
import org.mozilla.rocket.home.logoman.ui.LogoManNotification

class GetLogoManNotificationUseCase(private val logoManNotificationRepo: LogoManNotificationRepo) {

    operator fun invoke(): LogoManNotification.Notification? =
        logoManNotificationRepo.getNotification()?.toUiModel()
}

private fun Notification.toUiModel() = LogoManNotification.Notification(serialNumber, title, subtitle)