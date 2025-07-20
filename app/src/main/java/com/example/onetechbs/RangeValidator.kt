package com.example.onetechbs

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

/**
 * Calendar validator that rejects dates falling within the provided [blockedRanges].
 * Each range is a Pair of ISO strings (yyyy-MM-dd) for start and end (inclusive).
 * It also ensures dates are **not** in the past.
 */
class RangeValidator(private val blockedRanges: List<Pair<String, String>>) :
    CalendarConstraints.DateValidator, Parcelable {

    override fun isValid(date: Long): Boolean {
        // Disallow past dates
        val todayUtc = MaterialDatePicker.todayInUtcMilliseconds()
        if (date < todayUtc) return false

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = format.format(Date(date))

        // Reject if the selected day falls in any blocked range
        blockedRanges.forEach { (startStr, endStr) ->
            if (dateStr >= startStr && dateStr <= endStr) {
                return false
            }
        }
        return true
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(blockedRanges.size)
        blockedRanges.forEach { (start, end) ->
            dest.writeString(start)
            dest.writeString(end)
        }
    }

    private constructor(parcel: Parcel) : this(
        mutableListOf<Pair<String, String>>().apply {
            val size = parcel.readInt()
            repeat(size) {
                val start = parcel.readString() ?: ""
                val end = parcel.readString() ?: ""
                add(Pair(start, end))
            }
        }
    )

    companion object CREATOR : Parcelable.Creator<RangeValidator> {
        override fun createFromParcel(parcel: Parcel): RangeValidator = RangeValidator(parcel)
        override fun newArray(size: Int): Array<RangeValidator?> = arrayOfNulls(size)
    }
}
