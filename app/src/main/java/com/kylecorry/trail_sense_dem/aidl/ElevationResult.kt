package com.kylecorry.trail_sense_dem.aidl

import android.os.Parcel
import android.os.Parcelable

data class ElevationResult(
    val success: Boolean,
    val elevationMeters: Float,
    val errorMessage: String?
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readFloat(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (success) 1 else 0)
        parcel.writeFloat(elevationMeters)
        parcel.writeString(errorMessage)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ElevationResult> {
        override fun createFromParcel(parcel: Parcel): ElevationResult {
            return ElevationResult(parcel)
        }

        override fun newArray(size: Int): Array<ElevationResult?> {
            return arrayOfNulls(size)
        }
        
        fun success(elevationMeters: Float) = ElevationResult(
            success = true,
            elevationMeters = elevationMeters,
            errorMessage = null
        )
        
        fun error(errorMessage: String) = ElevationResult(
            success = false,
            elevationMeters = 0f,
            errorMessage = errorMessage
        )
    }
}
