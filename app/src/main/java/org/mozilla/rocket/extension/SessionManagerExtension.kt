package org.mozilla.rocket.extension

import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineSession

fun SessionManager.updateTrackingProtectionPolicy(policy: EngineSession.TrackingProtectionPolicy?) {
    sessions.forEach {
        getEngineSession(it)?.let { session ->
            session.settings.trackingProtectionPolicy = policy
        }
    }
}