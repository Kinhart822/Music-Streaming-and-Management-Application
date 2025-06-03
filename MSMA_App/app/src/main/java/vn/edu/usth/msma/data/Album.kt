package vn.edu.usth.msma.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: Long,
    val albumName: String,
    val description: String?,
    val releaseDate: String?,
    val albumTimeLength: Float?,
    val songIdList: List<Long>?,
    val songNameList: List<String>?,
    val artistIdList: List<Long>?,
    val artistNameList: List<String>?,
    val imageUrl: String?,
    val status: String?
) : Parcelable

