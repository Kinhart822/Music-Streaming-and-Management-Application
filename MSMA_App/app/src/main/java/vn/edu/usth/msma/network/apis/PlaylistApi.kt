package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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
}