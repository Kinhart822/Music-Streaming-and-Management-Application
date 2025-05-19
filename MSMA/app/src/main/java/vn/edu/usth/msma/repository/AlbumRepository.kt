package vn.edu.usth.msma.repository

import vn.edu.usth.msma.data.Album
import vn.edu.usth.msma.data.dto.response.management.AlbumResponse
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.utils.helpers.toAlbum

class AlbumRepository {
    private lateinit var album: Album
    private var albums: List<Album> = emptyList()

    fun updateAlbum(newAlbum: Album) {
        album = newAlbum
    }

    fun updateAlbums(newAlbums: List<Album>) {
        albums = newAlbums
    }

    fun updateSearchAlbums(newAlbumItem: List<ContentItem.AlbumItem>) {
        albums = newAlbumItem.map { it.toAlbum() }
    }

    fun updateAlbumResponseList(newAlbumItem: List<AlbumResponse>) {
        albums = newAlbumItem.map { it.toAlbum() }
    }

    fun getAllAlbums(): List<Album> {
        return albums
    }

    fun getAlbumById(albumId: Long): Album? {
        return albums.find { it.id == albumId }
    }

    fun clearAllAlbums() {
        albums = emptyList()
    }
}