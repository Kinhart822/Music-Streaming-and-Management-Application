package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.annotations.SerializedName

data class AlbumResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("albumName") val albumName: String,
    @SerializedName("description") val description: String,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("albumTimeLength") val albumTimeLength: Float,
    @SerializedName("songIdList") val songIdList: List<Long>,
    @SerializedName("songNameList") val songNameList: List<String>,
    @SerializedName("artistIdList") val artistIdList: List<Long>,
    @SerializedName("artistNameList") val artistNameList: List<String>,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("status") val status: String
)