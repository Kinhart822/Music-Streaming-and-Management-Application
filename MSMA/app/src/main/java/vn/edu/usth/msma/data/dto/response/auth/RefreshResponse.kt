package vn.edu.usth.msma.data.dto.response.auth

import com.google.gson.annotations.SerializedName

data class RefreshResponse(
    @SerializedName("accessToken")
    val accessToken: String
)