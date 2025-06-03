package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.annotations.SerializedName

data class PlaylistResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("playlistName") val playlistName: String,
    @SerializedName("playTimeLength") val playTimeLength: Float,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("songIdList") val songIdList: List<Long>,
    @SerializedName("songNameList") val songNameList: List<String>,
    @SerializedName("artistIdList") val artistIdList: List<Long>,
    @SerializedName("artistNameList") val artistNameList: List<String>,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String
)
