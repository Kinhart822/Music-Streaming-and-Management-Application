package vn.edu.usth.msma.data.dto.response.profile

import com.google.gson.annotations.SerializedName
import vn.edu.usth.msma.utils.constants.UserType

data class UserPresentation(
    @SerializedName("id")
    val id: Long,

    @SerializedName("avatar")
    val avatar: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("gender")
    val gender: String,

    @SerializedName("birthDay")
    val birthDay: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("createdBy")
    val createdBy: Long,

    @SerializedName("lastModifiedBy")
    val lastModifiedBy: Long,

    @SerializedName("createdDate")
    val createdDate: String,

    @SerializedName("lastModifiedDate")
    val lastModifiedDate: String,

    @SerializedName("userType")
    val userType: UserType
)