/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.util

import android.content.Context
import mozilla.components.service.fretboard.ExperimentDescriptor
import mozilla.components.service.fretboard.Fretboard
import org.mozilla.focus.FocusApplication

const val EXPERIMENTS_JSON_FILENAME = "experiments.json"
const val EXPERIMENTS_BASE_URL = "https://settings.prod.mozaws.net/v1"
const val EXPERIMENTS_BUCKET_NAME = "main"
const val EXPERIMENTS_COLLECTION_NAME = "lite-experiments"

const val EXPERIMENT_DESCRIPTOR_FIREBASE = "use-firebase"

lateinit var fretboard: Fretboard

val usingFirebase = ExperimentDescriptor(EXPERIMENT_DESCRIPTOR_FIREBASE)

val Context.app: FocusApplication
    get() = applicationContext as FocusApplication

