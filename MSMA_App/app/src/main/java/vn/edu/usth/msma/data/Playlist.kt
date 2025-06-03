package vn.edu.usth.msma.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val id: Long,
    val playlistName: String,
    val playTimeLength: Float?,
    val releaseDate: String?,
    val songIdList: List<Long>?,
    val songNameList: List<String>?,
    val artistIdList: List<Long>?,
    val artistNameList: List<String>?,
    val imageUrl: String?,
    val description: String?,
    val status: String?
) : Parcelable