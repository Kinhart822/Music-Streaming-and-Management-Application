package vn.edu.usth.msma.data.dto.request.auth

import com.google.gson.annotations.SerializedName

data class SignInRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)