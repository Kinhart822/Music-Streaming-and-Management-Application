package vn.edu.usth.msma.data.dto.request.management

import com.google.gson.annotations.SerializedName

data class HistoryListenResponse(
    @SerializedName("imageUrl")
    var imageUrl: String,

    @SerializedName("message")
    var message: String
)