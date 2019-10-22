package org.mozilla.rocket.content.common.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ContentTabTelemetryData(
    val vertical: String,
    val feed: String,
    val source: String,
    val category: String,
    val componentId: String,
    val subCategoryId: String,
    val versionId: Long,
    var sessionTime: Long = 0L,
    var urlCounts: Int = -1,
    var appLink: String = "null",
    var showKeyboard: Boolean = false
) : Parcelable