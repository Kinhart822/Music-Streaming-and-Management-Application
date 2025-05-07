package vn.edu.usth.msma.data.dto.response.management

import com.google.gson.annotations.SerializedName

data class GenreResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("briefDescription")
    val briefDescription: String,

    @SerializedName("fullDescription")
    val fullDescription: String,

    @SerializedName("createdDate")
    val createdDate: String,

    @SerializedName("lastModifiedDate")
    val lastModifiedDate: String
)