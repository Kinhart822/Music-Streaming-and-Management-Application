package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.annotations.SerializedName

data class SongResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("releaseDate")
    val releaseDate: String,

    @SerializedName("lyrics")
    val lyrics: String,

    @SerializedName("duration")
    val duration: String,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("downloadPermission")
    val downloadPermission: Boolean,

    @SerializedName("description")
    val description: String,

    @SerializedName("mp3Url")
    val mp3Url: String,

    @SerializedName("songStatus")
    val songStatus: String,

    @SerializedName("genreNameList")
    val genreNameList: List<String>,

    @SerializedName("artistNameList")
    val artistNameList: List<String>,

    @SerializedName("numberOfListeners")
    val numberOfListeners: Long,

    @SerializedName("countListen")
    val countListen: Long,

    @SerializedName("numberOfUserLike")
    val numberOfUserLike: Long,

    @SerializedName("numberOfDownload")
    val numberOfDownload: Long
)
