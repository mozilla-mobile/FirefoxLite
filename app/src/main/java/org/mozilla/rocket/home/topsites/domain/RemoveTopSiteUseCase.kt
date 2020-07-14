package org.mozilla.rocket.home.topsites.domain

import org.mozilla.rocket.home.data.ContentPrefRepo
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.toSiteModel

class RemoveTopSiteUseCase(
    private val topSitesRepo: TopSitesRepo,
    private val contentPrefRepo: ContentPrefRepo
) {

    suspend operator fun invoke(site: Site.UrlSite) {
        topSitesRepo.remove(site.toSiteModel(), contentPrefRepo.getContentPref().dataResId)
    }
}