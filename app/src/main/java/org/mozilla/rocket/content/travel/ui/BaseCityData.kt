package org.mozilla.rocket.content.travel.ui

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BaseCityData(
    val id: String,
    val name: String
) : Parcelable