package vn.edu.usth.msma.data.dto.request.auth

import com.google.gson.annotations.SerializedName

data class RefreshRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("email")
    val email: String
)