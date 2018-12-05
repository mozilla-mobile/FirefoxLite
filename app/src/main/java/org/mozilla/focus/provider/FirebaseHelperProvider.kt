/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mozilla.components.service.fretboard.Fretboard
import mozilla.components.service.fretboard.source.kinto.KintoExperimentSource
import mozilla.components.service.fretboard.storage.flatfile.FlatFileExperimentStorage
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.EXPERIMENTS_BASE_URL
import org.mozilla.rocket.util.EXPERIMENTS_BUCKET_NAME
import org.mozilla.rocket.util.EXPERIMENTS_COLLECTION_NAME
import org.mozilla.rocket.util.EXPERIMENTS_JSON_FILENAME
import org.mozilla.rocket.util.fretboard
import org.mozilla.rocket.util.usingFirebase
import java.io.File

class FirebaseHelperProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = this.context as Context

        GlobalScope.launch {
            async(IO) {
                loadExperiments(context)
            }.await()
            val enable = fretboard.isInExperiment(context, usingFirebase)
            FirebaseHelper.init(context, enable)
        }

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    companion object {
        private val TAG = "CrashlyticsInitProvider"
    }

    private fun loadExperiments(context: Context) {
        val experimentsFile = File(context.filesDir, EXPERIMENTS_JSON_FILENAME)
        val experimentSource = KintoExperimentSource(
            EXPERIMENTS_BASE_URL, EXPERIMENTS_BUCKET_NAME, EXPERIMENTS_COLLECTION_NAME
        )
        fretboard = Fretboard(experimentSource, FlatFileExperimentStorage(experimentsFile))
        fretboard.updateExperiments()
        fretboard.loadExperiments()

    }
}
