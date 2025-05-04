package vn.edu.usth.msma.data.dto.response.auth

import com.google.gson.annotations.SerializedName

data class OtpCheckResultResponse(
    @SerializedName("isValid")
    var isValid: Boolean
)
