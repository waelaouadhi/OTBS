package com.example.onetechbs.db
data class ProfilePictureDTO(
val type: String,
val picture: ByteArray
) {
    fun getBase64Image(): String {
        return "data:$type;base64," + android.util.Base64.encodeToString(picture, android.util.Base64.DEFAULT)
    }
}