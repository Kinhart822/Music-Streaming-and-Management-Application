package vn.edu.usth.msma.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import vn.edu.usth.msma.utils.constants.UserType

@Parcelize
data class Artist(
    val id: Long,
    val avatar: String?,
    val firstName: String?,
    val lastName: String?,
    val artistName: String?,
    val description: String?,
    val image: String?,
    val numberOfFollowers: Long?,
    val email: String?,
    val gender: String?,
    val birthDay: String?,
    val phone: String?,
    val status: Int?,
    val createdBy: Long?,
    val lastModifiedBy: Long?,
    val createdDate: String?,
    val lastModifiedDate: String?,
    val userType: UserType?,
    val artistSongIds: List<Long>?,
    val artistSongNameList: List<String>?,
    val artistPlaylistIds: List<Long>?,
    val artistPlaylistNameList: List<String>?,
    val artistAlbumIds: List<Long>?,
    val artistAlbumNameList: List<String>?,
) : Parcelable