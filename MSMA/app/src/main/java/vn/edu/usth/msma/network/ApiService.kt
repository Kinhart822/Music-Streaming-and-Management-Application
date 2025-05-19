package vn.edu.usth.msma.network

import vn.edu.usth.msma.network.apis.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiService @Inject constructor(
    private val apiClient: ApiClient
) {
    private val _unAuthApi: AuthApi by lazy {
        apiClient.getUnauthenticatedClient().create(AuthApi::class.java)
    }

    private val _authApi: AuthApi by lazy {
        apiClient.getAuthenticatedClient().create(AuthApi::class.java)
    }

    private val _unAccountApi: AccountApi by lazy {
        apiClient.getUnauthenticatedClient().create(AccountApi::class.java)
    }

    private val _accountApi: AccountApi by lazy {
        apiClient.getAuthenticatedClient().create(AccountApi::class.java)
    }

    private val _searchApi: SearchApi by lazy {
        apiClient.getAuthenticatedClient().create(SearchApi::class.java)
    }

    private val _songApi: SongApi by lazy {
        apiClient.getAuthenticatedClient().create(SongApi::class.java)
    }

    private val _artistApi: ArtistApi by lazy {
        apiClient.getAuthenticatedClient().create(ArtistApi::class.java)
    }

    private val _albumApi: AlbumApi by lazy {
        apiClient.getAuthenticatedClient().create(AlbumApi::class.java)
    }

    private val _playlistApi: PlaylistApi by lazy {
        apiClient.getAuthenticatedClient().create(PlaylistApi::class.java)
    }

    fun getUnAuthApi(): AuthApi = _unAuthApi

    fun getAuthApi(): AuthApi = _authApi

    fun getUnAccountApi(): AccountApi = _unAccountApi

    fun getAccountApi(): AccountApi = _accountApi

    fun getSearchApi(): SearchApi = _searchApi

    fun getSongApi(): SongApi = _songApi

    fun getArtistApi(): ArtistApi = _artistApi

    fun getAlbumApi(): AlbumApi = _albumApi

    fun getPlaylistApi(): PlaylistApi = _playlistApi
}