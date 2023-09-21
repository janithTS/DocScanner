package org.sdase.submission.documentscanner.models

import android.os.Parcel
import android.os.Parcelable

class PointDouble(var x: Double = 0.0, var y: Double = 0.0): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    ) {
    }

    constructor(vals: DoubleArray?): this() {
        set(vals)
    }

    fun set(vals: DoubleArray?) {
        if (vals != null) {
            x = if (vals.isNotEmpty()) vals[0] else 0.0
            y = if (vals.size > 1) vals[1] else 0.0
        } else {
            x = 0.0
            y = 0.0
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(x)
        parcel.writeDouble(y)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PointDouble> {
        override fun createFromParcel(parcel: Parcel): PointDouble {
            return PointDouble(parcel)
        }

        override fun newArray(size: Int): Array<PointDouble?> {
            return arrayOfNulls(size)
        }
    }
}