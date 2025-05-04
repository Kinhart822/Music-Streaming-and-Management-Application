package vn.edu.usth.msma.data.dto.response.auth

import com.google.gson.annotations.SerializedName

data class JwtAuthenticationResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)
