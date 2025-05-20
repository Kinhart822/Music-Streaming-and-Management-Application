package vn.edu.usth.msma.network.apis

import retrofit2.Response
import retrofit2.http.GET
import vn.edu.usth.msma.data.dto.response.management.AlbumResponse
import vn.edu.usth.msma.data.dto.response.management.HistoryListenResponse
import vn.edu.usth.msma.data.dto.response.management.PlaylistResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.data.dto.response.profile.ArtistPresentation

interface HomeApi {
    // Card
    @GET("/song/top10/trending")
    suspend fun getTop10TrendingSongs(): Response<List<SongResponse>>

    @GET("/song/top15/download")
    suspend fun getTop15DownloadedSongs(): Response<List<SongResponse>>

    @GET("/api/v1/user/playlist/recent-playlists")
    suspend fun getRecentPlaylists(): Response<List<PlaylistResponse>>

    @GET("/api/v1/user/album/recent-albums")
    suspend fun getRecentAlbums(): Response<List<AlbumResponse>>

    @GET("/api/v1/account/recentViewArtist")
    suspend fun getRecentFollowedArtistsOfUser(): Response<List<ArtistPresentation>>

    // Main
    @GET("/api/v1/account/recentListening")
    suspend fun recentListening(): Response<List<HistoryListenResponse>>
}