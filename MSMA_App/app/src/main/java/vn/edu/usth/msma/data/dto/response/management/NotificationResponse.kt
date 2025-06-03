package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.annotations.SerializedName

data class NotificationResponse (
    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("createdDate")
    val createdDate: String,
)