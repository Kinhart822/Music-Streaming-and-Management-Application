package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse

interface SearchApi {
    @GET("/api/v1/account/getAllGenre")
    suspend fun getAllGenres(): Response<List<GenreResponse>>

    @GET("/api/v1/account/song/genre/{genreId}")
    suspend fun getSongsByGenre(
        @Path("genreId") genreId: Long
    ): Response<List<SongResponse>>

    @GET("/api/v1/search/contents")
    suspend fun getAllContents(
        @Query("title") title: String? = null,
        @Query("genreId") genreId: Long? = null,
        @Query("type") type: String = "all",        // All, Songs, Playlists, Albums, Artists
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<Map<String, Any>>
}