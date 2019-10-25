package org.mozilla.rocket.content.common.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TabSwipeTelemetryData(
    val vertical: String,
    val feed: String,
    val source: String,
    var sessionTime: Long = 0L,
    var urlCounts: Int = 0,
    var appLink: String = "null",
    var showKeyboard: Boolean = false
) : Parcelable