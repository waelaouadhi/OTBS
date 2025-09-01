package com.example.onetechbs.db

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class JobOfferResponseDTO(
    val id: Long,
    val title: String,
    @SerializedName("summary") val description: String,
    val department: String,
    val responsibilities: List<String>?,
    @SerializedName("qualifications_required") val qualificationsRequired: List<String>?,
    @SerializedName("qualifications_preferred") val qualificationsPreferred: List<String>?,
    val role: String?,
    @SerializedName(value = "isApplied", alternate = ["applied"]) val isApplied: Boolean,
    val numberOfApplications: Int,
    val status: String,
    val applicationStatus: String?,
    val isInternal: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("what_we_offer") val whatWeOffer: String? = null,
    val criteria: List<CriterionDTO>? = null
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(department)
        dest.writeStringList(responsibilities ?: emptyList())
        dest.writeStringList(qualificationsRequired ?: emptyList())
        dest.writeStringList(qualificationsPreferred ?: emptyList())
        dest.writeString(role)
        dest.writeByte(if (isApplied) 1 else 0)
        dest.writeInt(numberOfApplications)
        dest.writeString(status)
        dest.writeString(applicationStatus)
        dest.writeByte(if (isInternal) 1 else 0)
        dest.writeString(createdAt)
        dest.writeString(updatedAt)
        dest.writeString(whatWeOffer)
        // criteria omitted in parcel for brevity
    }

    companion object CREATOR : Parcelable.Creator<JobOfferResponseDTO> {
        override fun createFromParcel(parcel: Parcel): JobOfferResponseDTO {
            return JobOfferResponseDTO(
                parcel.readLong(),
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                mutableListOf<String>().apply { parcel.readStringList(this) },
                mutableListOf<String>().apply { parcel.readStringList(this) },
                mutableListOf<String>().apply { parcel.readStringList(this) },
                parcel.readString(),
                parcel.readByte() != 0.toByte(),
                parcel.readInt(),
                parcel.readString() ?: "",
                parcel.readString(),
                parcel.readByte() != 0.toByte(),
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString(),
                null
            )
        }

        override fun newArray(size: Int): Array<JobOfferResponseDTO?> {
            return arrayOfNulls(size)
        }
    }
}