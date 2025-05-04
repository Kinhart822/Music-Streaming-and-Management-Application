package vn.edu.usth.msma.data.dto.request.auth

import com.google.gson.annotations.SerializedName

data class SignUpRequest(
    @SerializedName("email")
    var email: String,

    @SerializedName("password")
    var password: String
)
