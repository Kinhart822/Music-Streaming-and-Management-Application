package vn.edu.usth.msma.data.dto.request.auth

import com.google.gson.annotations.SerializedName

data class NewPasswordRequest(
    @SerializedName("sessionId")
    var sessionId: String,

    @SerializedName("password")
    var password: String
)
