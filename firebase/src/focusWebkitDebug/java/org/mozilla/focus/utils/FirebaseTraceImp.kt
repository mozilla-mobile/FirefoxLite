/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * It's a wrapper to communicate with Firebase Monitoring
 */
class FirebaseTraceImp(private val traceKey: String) : FirebaseContract.FirebaseTrace {

    private val trace: Trace = FirebasePerformance.getInstance().newTrace(traceKey)

    override fun getKey(): String = traceKey

    override fun start() {
        trace.start()
    }

    override fun stop() {
        trace.stop()
    }
}