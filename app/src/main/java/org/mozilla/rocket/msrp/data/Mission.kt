package org.mozilla.rocket.msrp.data

import android.os.Parcel
import android.os.Parcelable

data class Mission(
    val mid: String,
    val missionType: String,
    val missionName: String,

    val title: String,
    val description: String,
    val imageUrl: String,

    val endpoint: String, // Use this API to request to join the mission
    val redeem: String?, // Use this API to retrieve the reward

    val events: List<String>,
    val important: Boolean,
    val minVersion: Int,
    val minVerDialogTitle: String,
    val minVerDialogMessage: String,
    val minVerDialogImage: String,

    val joinEndDate: Long,
    val expiredDate: Long,
    val redeemEndDate: Long,
    val rewardExpiredDate: Long,

    val status: Int, // STATUS_NEW, STATUS_NEW, STATUS_REDEEMABLE, STATUS_REDEEMED
    val missionProgress: MissionProgress?,

    val totalDays: Int
) : Parcelable {
    var unread = true
    val uniqueId: String
        get() = "$missionType$mid"

    constructor(source: Parcel) : this(
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString(),
        source.createStringArrayList()!!,
        1 == source.readInt(),
        source.readInt(),
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readLong(),
        source.readLong(),
        source.readLong(),
        source.readLong(),
        source.readInt(),
        source.readParcelable<MissionProgress.TypeDaily>(MissionProgress.TypeDaily::class.java.classLoader),
        source.readInt()
    ) {
        unread = 1 == source.readInt()
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(mid)
        writeString(missionType)
        writeString(title)
        writeString(missionName)
        writeString(description)
        writeString(imageUrl)
        writeString(endpoint)
        writeString(redeem)
        writeStringList(events)
        writeInt((if (important) 1 else 0))
        writeInt(minVersion)
        writeString(minVerDialogTitle)
        writeString(minVerDialogMessage)
        writeString(minVerDialogImage)
        writeLong(joinEndDate)
        writeLong(expiredDate)
        writeLong(redeemEndDate)
        writeLong(rewardExpiredDate)
        writeInt(status)
        writeParcelable(missionProgress, flags)
        writeInt(totalDays)
        writeInt(if (unread) 1 else 0)
    }

    companion object {
        const val STATUS_NEW = 0

        const val STATUS_JOINED = 1

        const val STATUS_REDEEMABLE = 2

        const val STATUS_REDEEMED = 3

        @JvmField
        val CREATOR: Parcelable.Creator<Mission> = object : Parcelable.Creator<Mission> {
            override fun createFromParcel(source: Parcel): Mission = Mission(source)
            override fun newArray(size: Int): Array<Mission?> = arrayOfNulls(size)
        }
    }
}

sealed class MissionProgress : Parcelable {
    data class TypeDaily(
        val joinDate: Long, // the date the user join this mission
        val currentDay: Int, // number of the total days accomplished
        val totalDays: Int, // number of the total days needed
        val message: String
    ) : MissionProgress() {
        constructor(source: Parcel) : this(
            source.readLong(),
            source.readInt(),
            source.readInt(),
            source.readString()!!
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
            writeLong(joinDate)
            writeInt(currentDay)
            writeInt(totalDays)
            writeString(message)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<TypeDaily> = object : Parcelable.Creator<TypeDaily> {
                override fun createFromParcel(source: Parcel): TypeDaily = TypeDaily(source)
                override fun newArray(size: Int): Array<TypeDaily?> = arrayOfNulls(size)
            }
        }
    }
}