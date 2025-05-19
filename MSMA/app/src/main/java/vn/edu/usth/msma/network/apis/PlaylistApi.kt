package vn.edu.usth.msma.network.apis

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import vn.edu.usth.msma.data.dto.request.management.AddSongRequest
import vn.edu.usth.msma.data.dto.request.management.RemoveSongRequest
import vn.edu.usth.msma.data.dto.response.auth.ApiResponse
import vn.edu.usth.msma.data.dto.response.management.PlaylistResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse

interface PlaylistApi {
    // View Playlist
    @GET("/api/v1/user/playlist/info/{id}")
    suspend fun getPlaylist(@Path("id") id: Long): Response<PlaylistResponse>

    @GET("/api/v1/user/viewArtistProfile/playlist/{id}/songs")
    suspend fun getPlaylistSongs(@Path("id") id: Long): Response<List<SongResponse>>

    // Manage artist playlist
    @GET("/api/v1/user/playlist/saved-playlists")
    suspend fun getAllUserSavedPlaylists(): Response<List<PlaylistResponse>>

    @POST("/api/v1/user/playlist/userSavePlaylist/{id}")
    suspend fun saveArtistPlaylist(@Path("id") id: Long): Response<ApiResponse>

    @DELETE("/api/v1/user/playlist/userUnSavePlaylist/{id}")
    suspend fun unSaveArtistPlaylist(@Path("id") id: Long): Response<ApiResponse>

    @GET("/api/v1/user/playlist/saved-playlist-check/{id}")
    suspend fun checkSavedPlaylist(@Path("id") id: Long): Response<Boolean>

    // Manage User Playlist
    @POST("/api/v1/user/playlist/create")
    suspend fun createPlaylist(@Path("id") id: Long): Response<PlaylistResponse>

    @DELETE("/api/v1/user/playlist/delete/{id}")
    suspend fun deletePlaylist(@Path("id") id: Long): Response<ApiResponse>

    @Multipart
    @PUT("/api/v1/user/playlist/update/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: Long,
        @Part("playlistName") playlistName: RequestBody,
        @Part("description") description: RequestBody,
        @Part("songIds") songIds: List<RequestBody>,
        @Part("artistIds") artistIds: List<RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<PlaylistResponse>

    // Manage Song in Playlist
    @POST("/api/v1/user/playlist/songList/add")
    suspend fun addListSong(@Body addSongRequest: AddSongRequest): Response<ApiResponse>

    @DELETE("/api/v1/user/playlist/songList/remove")
    suspend fun removeListSong(@Body removeSongRequest: RemoveSongRequest): Response<ApiResponse>
}