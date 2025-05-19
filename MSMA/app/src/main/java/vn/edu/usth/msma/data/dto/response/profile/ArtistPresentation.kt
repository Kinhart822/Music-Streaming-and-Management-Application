package vn.edu.usth.msma.data.dto.response.profile

import com.google.gson.annotations.SerializedName
import vn.edu.usth.msma.utils.constants.UserType

data class ArtistPresentation(
    @SerializedName("id")
    val id: Long,
    @SerializedName("avatar")
    val avatar: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("artistName")
    val artistName: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("image")
    val image: String,
    @SerializedName("numberOfFollowers")
    val numberOfFollowers: Long,
    @SerializedName("email")
    val email: String,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("birthDay")
    val birthDay: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("createdBy")
    val createdBy: Long,
    @SerializedName("lastModifiedBy")
    val lastModifiedBy: Long,
    @SerializedName("createdDate")
    val createdDate: String,
    @SerializedName("lastModifiedDate")
    val lastModifiedDate: String,
    @SerializedName("userType")
    val userType: UserType,
    @SerializedName("artistSongIds")
    val artistSongIds: List<Long>,
    @SerializedName("artistSongNameList")
    val artistSongNameList: List<String>,
    @SerializedName("artistPlaylistIds")
    val artistPlaylistIds: List<Long>,
    @SerializedName("artistPlaylistNameList")
    val artistPlaylistNameList: List<String>,
    @SerializedName("artistAlbumIds")
    val artistAlbumIds: List<Long>,
    @SerializedName("artistAlbumNameList")
    val artistAlbumNameList: List<String>
)