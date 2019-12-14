package org.mozilla.rocket.content.travel.ui

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Locale

@Parcelize
data class BaseCityData(
    val id: String,
    val name: String,
    val type: String,
    val nameInEnglish: String,
    val countryCode: String
) : Parcelable {
    fun getTelemetryItemName() = String.format("%s-%s", countryCode, nameInEnglish.toLowerCase(Locale.getDefault()))
}