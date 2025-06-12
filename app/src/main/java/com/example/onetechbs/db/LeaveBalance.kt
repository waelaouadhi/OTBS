package com.example.onetechbs.db

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class LeaveBalance(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("userDn")
    val userDn: String,

    @SerializedName("totalLeave")
    val totalLeave: Int,

    @SerializedName("usedLeave")
    val usedLeave: Int,

    @SerializedName("remainingLeave")
    val remainingLeave: Int,

    @SerializedName("lastUpdatedDate")
    val lastUpdatedDate: LocalDate
) {
    companion object {
        fun createEmpty(userDn: String): LeaveBalance {
            return LeaveBalance(
                userDn = userDn,
                totalLeave = 0,
                usedLeave = 0,
                remainingLeave = 0,
                lastUpdatedDate = LocalDate.now()
            )
        }
    }

    fun addMonthlyLeave(): LeaveBalance {
        val now = LocalDate.now()
        return if (lastUpdatedDate.plusMonths(1).isBefore(now)) {
            copy(
                totalLeave = totalLeave + 2,
                remainingLeave = totalLeave + 2 - usedLeave,
                lastUpdatedDate = now
            )
        } else {
            this
        }
    }

    fun calculateRemainingLeave(): Int {
        return totalLeave - usedLeave
    }

    fun hasAvailableLeave(days: Int): Boolean {
        return remainingLeave >= days
    }

    fun useLeave(days: Int): LeaveBalance {
        if (!hasAvailableLeave(days)) {
            throw IllegalStateException("Not enough leave balance")
        }
        return copy(
            usedLeave = usedLeave + days,
            remainingLeave = remainingLeave - days
        )
    }
} 