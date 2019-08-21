package org.mozilla.rocket.home.topsites.domain

import org.mozilla.rocket.home.topsites.data.TopSitesRepo

class TopSitesConfigsUseCase(private val topSitesRepo: TopSitesRepo) {

    operator fun invoke(): Configs = Configs(isPinEnabled = topSitesRepo.isPinEnabled())

    data class Configs(val isPinEnabled: Boolean)
}