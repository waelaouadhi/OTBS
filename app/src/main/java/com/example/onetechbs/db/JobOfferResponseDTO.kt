package com.example.onetechbs.db

import android.os.Parcel
import android.os.Parcelable
import java.time.LocalDateTime

data class JobOfferResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val department: String,
    val responsibilities: String,
    val qualifications: String,
    val role: String,
    val isApplied: Boolean,
    val numberOfApplications: Int,
    val status: String,
    val applicationStatus: String,
    val isInternal: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(department)
        dest.writeString(responsibilities)
        dest.writeString(qualifications)
        dest.writeString(role)
        dest.writeByte(if (isApplied) 1 else 0)
        dest.writeInt(numberOfApplications)
        dest.writeString(status)
        dest.writeString(applicationStatus)
        dest.writeByte(if (isInternal) 1 else 0)
        dest.writeSerializable(createdAt)
        dest.writeSerializable(updatedAt)
    }

    companion object CREATOR : Parcelable.Creator<JobOfferResponseDTO> {
        override fun createFromParcel(parcel: Parcel): JobOfferResponseDTO {
            return JobOfferResponseDTO(
                parcel.readLong(),
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readByte() != 0.toByte(),
                parcel.readInt(),
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readByte() != 0.toByte(),
                parcel.readSerializable() as LocalDateTime,
                parcel.readSerializable() as LocalDateTime
            )
        }

        override fun newArray(size: Int): Array<JobOfferResponseDTO?> {
            return arrayOfNulls(size)
        }
    }
}