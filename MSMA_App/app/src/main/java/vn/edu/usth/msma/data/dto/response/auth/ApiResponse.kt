package vn.edu.usth.msma.data.dto.response.auth

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("status")
    var status: String,

    @SerializedName("message")
    var message: String,

    @SerializedName("description")
    var description: String
)
