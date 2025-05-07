package vn.edu.usth.msma.data.dto.request.profile

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("newPassword")
    var newPassword: String,

    @SerializedName("confirmPassword")
    var confirmPassword: String,
)