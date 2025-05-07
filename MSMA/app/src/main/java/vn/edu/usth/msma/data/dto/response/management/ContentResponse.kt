package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import vn.edu.usth.msma.utils.constants.UserType
import java.lang.reflect.Type

data class ContentResponse(
    @SerializedName("content")
    val content: List<ContentItem>
)

sealed class ContentItem {
    data class SongItem(
        @SerializedName("id") val id: Long,
        @SerializedName("title") val title: String,
        @SerializedName("releaseDate") val releaseDate: String?,
        @SerializedName("lyrics") val lyrics: String?,
        @SerializedName("duration") val duration: String?,
        @SerializedName("imageUrl") val imageUrl: String?,
        @SerializedName("downloadPermission") val downloadPermission: Boolean?,
        @SerializedName("description") val description: String?,
        @SerializedName("mp3Url") val mp3Url: String?,
        @SerializedName("songStatus") val songStatus: String?,
        @SerializedName("genreNameList") val genreNameList: List<String>?,
        @SerializedName("artistNameList") val artistNameList: List<String>?,
        @SerializedName("numberOfListeners") val numberOfListeners: Long?,
        @SerializedName("countListen") val countListen: Long?,
        @SerializedName("numberOfUserLike") val numberOfUserLike: Long?,
        @SerializedName("numberOfDownload") val numberOfDownload: Long?
    ) : ContentItem()

    data class PlaylistItem(
        @SerializedName("id") val id: Long,
        @SerializedName("playlistName") val playlistName: String,
        @SerializedName("playTimeLength") val playTimeLength: Float?,
        @SerializedName("releaseDate") val releaseDate: String?,
        @SerializedName("songNameList") val songNameList: List<String>?,
        @SerializedName("artistNameList") val artistNameList: List<String>?,
        @SerializedName("imageUrl") val imageUrl: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("status") val status: String?
    ) : ContentItem()

    data class AlbumItem(
        @SerializedName("id") val id: Long,
        @SerializedName("albumName") val albumName: String,
        @SerializedName("description") val description: String?,
        @SerializedName("releaseDate") val releaseDate: String?,
        @SerializedName("albumTimeLength") val albumTimeLength: Float?,
        @SerializedName("songNameList") val songNameList: List<String>?,
        @SerializedName("artistNameList") val artistNameList: List<String>?,
        @SerializedName("imageUrl") val imageUrl: String?,
        @SerializedName("status") val status: String?
    ) : ContentItem()

    data class ArtistItem(
        @SerializedName("id")
        val id: Long,
        @SerializedName("avatar")
        val avatar: String?,
        @SerializedName("firstName")
        val firstName: String?,
        @SerializedName("lastName")
        val lastName: String?,
        @SerializedName("artistName")
        val artistName: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("image")
        val image: String?,
        @SerializedName("backgroundImage")
        val backgroundImage: String?,
        @SerializedName("countListen")
        val countListen: Long?,
        @SerializedName("numberOfFollowers")
        val numberOfFollowers: Long?,
        @SerializedName("email")
        val email: String?,
        @SerializedName("gender")
        val gender: String?,
        @SerializedName("birthDay")
        val birthDay: String?,
        @SerializedName("phone")
        val phone: String?,
        @SerializedName("status")
        val status: Int?,
        @SerializedName("createdBy")
        val createdBy: Long?,
        @SerializedName("lastModifiedBy")
        val lastModifiedBy: Long?,
        @SerializedName("createdDate")
        val createdDate: String?,
        @SerializedName("lastModifiedDate")
        val lastModifiedDate: String?,
        @SerializedName("userType")
        val userType: UserType?,
        @SerializedName("artistSongIds")
        val artistSongIds: List<Long>?,
        @SerializedName("artistPlaylistIds")
        val artistPlaylistIds: List<Long>?,
        @SerializedName("artistAlbumIds")
        val artistAlbumIds: List<Long>?
    ) : ContentItem()
}

class ContentItemDeserializer : JsonDeserializer<ContentItem> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ContentItem {
        val jsonObject = json.asJsonObject
        return when {
            jsonObject.has("title") -> context.deserialize(
                jsonObject,
                ContentItem.SongItem::class.java
            )

            jsonObject.has("playlistName") -> context.deserialize(
                jsonObject,
                ContentItem.PlaylistItem::class.java
            )

            jsonObject.has("albumName") -> context.deserialize(
                jsonObject,
                ContentItem.AlbumItem::class.java
            )

            jsonObject.has("artistName") -> context.deserialize(
                jsonObject,
                ContentItem.ArtistItem::class.java
            )

            else -> throw JsonParseException("Unknown content type: $jsonObject")
        }
    }
}

class ContentResponseDeserializer : JsonDeserializer<ContentResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ContentResponse {
        val jsonObject = json.asJsonObject
        val contentArray = jsonObject.getAsJsonArray("content")
        val contentList =
            contentArray.map { context.deserialize<ContentItem>(it, ContentItem::class.java) }
        return ContentResponse(content = contentList)
    }
}