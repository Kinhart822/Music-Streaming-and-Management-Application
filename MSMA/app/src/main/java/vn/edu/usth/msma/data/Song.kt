package vn.edu.usth.msma.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val releaseDate: String?,
    val lyrics: String?,
    val duration: String?,
    val imageUrl: String?,
    val downloadPermission: Boolean?,
    val description: String?,
    val mp3Url: String?,
    val songStatus: String?,
    val genreNameList: List<String>?,
    val artistNameList: List<String>?,
    val numberOfListeners: Long?,
    val countListen: Long?,
    val numberOfUserLike: Long?,
    val numberOfDownload: Long?
) : Parcelable