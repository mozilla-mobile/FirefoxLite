package org.mozilla.rocket.urlinput

import android.arch.lifecycle.LiveData

interface SearchPortalDataSource {

    fun fetchPortals(): LiveData<List<SearchPortal>>
}
