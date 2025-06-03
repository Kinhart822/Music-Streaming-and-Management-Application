package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import vn.edu.usth.msma.data.dto.response.auth.ApiResponse
import vn.edu.usth.msma.data.dto.response.management.AlbumResponse
import vn.edu.usth.msma.data.dto.response.management.PlaylistResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.data.dto.response.profile.ArtistPresentation

interface ArtistApi {
    // View Artist Profile
    @GET("/api/v1/user/viewArtistProfile/{id}")
    suspend fun viewArtistProfile(@Path("id") id: Long): Response<ArtistPresentation>

    @GET("/api/v1/user/viewArtistProfile/{id}/songs")
    suspend fun getArtistSongs(@Path("id") id: Long): Response<List<SongResponse>>

    @GET("/api/v1/user/viewArtistProfile/{id}/playlists")
    suspend fun getArtistPlaylists(@Path("id") id: Long): Response<List<PlaylistResponse>>

    @GET("/api/v1/user/viewArtistProfile/{id}/albums")
    suspend fun getArtistAlbums(@Path("id") id: Long): Response<List<AlbumResponse>>

    // Followed Artist
    @GET("/api/v1/user/followed-artists")
    suspend fun getFollowedArtists(): Response<List<ArtistPresentation>>

    @POST("/api/v1/user/follow/artist/{artistId}")
    suspend fun followArtist(@Path("artistId") artistId: Long): Response<ApiResponse>

    @DELETE("/api/v1/user/unfollow/artist/{artistId}")
    suspend fun unfollowArtist(@Path("artistId") artistId: Long): Response<ApiResponse>

    @GET("/api/v1/user/followed-check/{artistId}")
    suspend fun checkFollowedArtist(@Path("artistId") artistId: Long): Response<Boolean>
}