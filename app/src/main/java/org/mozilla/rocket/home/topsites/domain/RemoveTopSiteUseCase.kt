package org.mozilla.rocket.home.topsites.domain

import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.toSiteModel

class RemoveTopSiteUseCase(private val topSitesRepo: TopSitesRepo) {

    suspend operator fun invoke(site: Site) {
        topSitesRepo.remove(site.toSiteModel())
    }
}