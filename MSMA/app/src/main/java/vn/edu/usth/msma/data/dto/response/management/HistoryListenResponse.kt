package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.annotations.SerializedName

data class HistoryListenResponse (
    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("songName")
    val songName: String,

    @SerializedName("message")
    val message: String
)

