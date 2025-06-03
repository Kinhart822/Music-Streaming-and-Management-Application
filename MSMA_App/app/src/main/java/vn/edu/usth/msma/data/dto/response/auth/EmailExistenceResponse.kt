package vn.edu.usth.msma.data.dto.response.auth

import com.google.gson.annotations.SerializedName

data class EmailExistenceResponse(
    @SerializedName("emailExisted")
    var emailExisted: Boolean
)
