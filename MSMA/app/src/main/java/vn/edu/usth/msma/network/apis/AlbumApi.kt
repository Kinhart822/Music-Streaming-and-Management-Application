package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import vn.edu.usth.msma.data.dto.response.auth.ApiResponse
import vn.edu.usth.msma.data.dto.response.management.AlbumResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse

interface AlbumApi {
    // View album
    @GET("/api/v1/user/album/info/{id}")
    suspend fun getAlbum(@Path("id") id: Long): Response<AlbumResponse>

    @GET("/api/v1/user/viewArtistProfile/album/{id}/songs")
    suspend fun getAlbumSongs(@Path("id") id: Long): Response<List<SongResponse>>

    // Manage artist album
    @GET("/api/v1/user/album/saved-albums")
    suspend fun getAllUserSavedAlbums(): Response<List<AlbumResponse>>

    @POST("/api/v1/user/album/userSaveAlbum/{id}")
    suspend fun saveArtistAlbum(@Path("id") id: Long): Response<ApiResponse>

    @DELETE("/api/v1/user/album/userUnSaveAlbum/{id}")
    suspend fun unSaveArtistAlbum(@Path("id") id: Long): Response<ApiResponse>

    @GET("/api/v1/user/album/saved-album-check/{id}")
    suspend fun checkSavedAlbum(@Path("id") id: Long): Response<Boolean>
}