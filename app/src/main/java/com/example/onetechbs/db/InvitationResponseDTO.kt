package com.example.onetechbs.db

import android.os.Parcelable


data class InvitationResponseDTO(


val id: Long,
val employeeName: String,
val status: EStatus,// or use enum EStatus if you have one
val employeeId: String
)

