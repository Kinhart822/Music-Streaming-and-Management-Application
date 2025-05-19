package vn.edu.usth.msma.repository

import vn.edu.usth.msma.data.Playlist
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.management.PlaylistResponse
import vn.edu.usth.msma.utils.helpers.toPlaylist

class PlaylistRepository {
    private lateinit var playlist: Playlist
    private var playlists: List<Playlist> = emptyList()

    fun updatePlaylist(newPlaylist: Playlist) {
        playlist = newPlaylist
    }

    fun updatePlaylists(newPlaylists: List<Playlist>) {
        playlists = newPlaylists
    }

    fun updateSearchPlaylists(newPlaylistItem: List<ContentItem.PlaylistItem>) {
        playlists = newPlaylistItem.map { it.toPlaylist() }
    }

    fun updatePlaylistResponseList(newPlaylistItem: List<PlaylistResponse>) {
        playlists = newPlaylistItem.map { it.toPlaylist() }
    }

    fun getAllPlaylists(): List<Playlist> {
        return playlists
    }

    fun getPlaylistById(playlistId: Long): Playlist? {
        return playlists.find { it.id == playlistId }
    }

    fun clearAllPlaylists() {
        playlists = emptyList()
    }
}