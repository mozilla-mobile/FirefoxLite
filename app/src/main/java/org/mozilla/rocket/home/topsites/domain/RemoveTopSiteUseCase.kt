package org.mozilla.rocket.home.topsites.domain

import org.mozilla.rocket.abtesting.LocalAbTesting
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.toSiteModel

class RemoveTopSiteUseCase(private val topSitesRepo: TopSitesRepo) {

    suspend operator fun invoke(site: Site.UrlSite) {
        // TODO: Remove after top site AB testing finished
        if (LocalAbTesting.isExperimentEnabled(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES) &&
                LocalAbTesting.checkAssignedBucket(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES) != null) {
            topSitesRepo.removeAbTesting(site.toSiteModel())
        } else {
            topSitesRepo.remove(site.toSiteModel())
        }
    }
}