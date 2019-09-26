package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.Mission

class HasUnreadMissionsUseCase {

    operator fun invoke(missions: List<Mission>): Boolean =
            missions.count { it.unread } > 0
}