/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import android.arch.lifecycle.LiveData

class SearchPortalRepository(private val globalDataSource: SearchPortalDataSource, private val localeDataSource: SearchPortalDataSource)/*: SearchPortalDataSource */ {

    fun fetchGlobal(): LiveData<List<SearchPortal>> {
        return globalDataSource.fetchPortals()
    }

    fun fetchLocale(): LiveData<List<SearchPortal>> {
        return localeDataSource.fetchPortals()
    }

    companion object {

        @Volatile private var INSTANCE: SearchPortalRepository? = null

        @JvmStatic
        fun getInstance(globalDataSource: SearchPortalDataSource, localeDataSource: SearchPortalDataSource): SearchPortalRepository? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: SearchPortalRepository(globalDataSource, localeDataSource).also {
                        INSTANCE = it
                    }
                }
    }
}