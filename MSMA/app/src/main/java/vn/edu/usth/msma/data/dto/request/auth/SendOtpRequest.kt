package vn.edu.usth.msma.data.dto.request.auth

import com.google.gson.annotations.SerializedName

data class SendOtpRequest(
    @SerializedName("sessionId")
    var sessionId: String,

    @SerializedName("email")
    var email: String
)
