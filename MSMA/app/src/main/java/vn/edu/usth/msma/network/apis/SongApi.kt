package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import vn.edu.usth.msma.data.dto.response.auth.ApiResponse
import vn.edu.usth.msma.data.dto.response.management.HistoryListenResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse

interface SongApi {
    // Song Listen History
    @POST("/api/v1/user/listen/{songId}")
    suspend fun recordSongListen(@Path("songId") songId: Long): Response<ApiResponse>

    // Favourite Songs
    @POST("/api/v1/user/likeSong/{songId}")
    suspend fun userLikeSong(@Path("songId") songId: Long): Response<ApiResponse>

    @DELETE("/api/v1/user/unlikeSong/{songId}")
    suspend fun userUnlikeSong(@Path("songId") songId: Long): Response<ApiResponse>

    @GET("/api/v1/user/liked-songs")
    suspend fun getLikedSongs(): Response<List<SongResponse>>

    @GET("/api/v1/user/liked-check/{songId}")
    suspend fun checkLikedSongs(@Path("songId") songId: Long): Response<Boolean>

    // Download Songs
    @POST("/api/v1/user/downloadSong/{songId}")
    suspend fun userDownloadSong(@Path("songId") songId: Long): Response<ApiResponse>
}
