package vn.edu.usth.msma.data.dto.request.auth

import com.google.gson.annotations.SerializedName

data class CheckOtpRequest(
    @SerializedName("sessionId")
    var sessionId: String,

    @SerializedName("otp")
    var otp: String
)
