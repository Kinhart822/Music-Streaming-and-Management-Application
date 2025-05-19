package vn.edu.usth.msma.data.dto.request.management

import com.google.gson.annotations.SerializedName

data class RemoveSongRequest(
    @SerializedName("playlistId")
    var playlistId: Long?,

    @SerializedName("albumId")
    var albumId: Long?,

    @SerializedName("songId")
    var songId: Long?,

    @SerializedName("songIdList")
    var songIdList: List<Long>?,
)