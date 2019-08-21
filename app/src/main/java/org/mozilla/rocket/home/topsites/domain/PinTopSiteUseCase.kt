package org.mozilla.rocket.home.topsites.domain

import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site

class PinTopSiteUseCase(private val topSitesRepo: TopSitesRepo) {

    operator fun invoke(site: Site) {
        topSitesRepo.pin(site.toSiteModel())
    }
}

private fun Site.toSiteModel(): org.mozilla.focus.history.model.Site =
        org.mozilla.focus.history.model.Site(
            id,
            title,
            url,
            viewCount,
            lastViewTimestamp,
            iconUri
        ).apply {
            isDefault = when (this@toSiteModel) {
                is Site.FixedSite -> true
                is Site.RemovableSite -> this@toSiteModel.isDefault
            }
        }