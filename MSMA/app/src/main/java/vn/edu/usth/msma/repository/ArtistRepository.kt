package vn.edu.usth.msma.repository

import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.profile.ArtistPresentation
import vn.edu.usth.msma.utils.helpers.toArtist

class ArtistRepository {
    private lateinit var artist: Artist
    private var artists: List<Artist> = emptyList()

    fun updateArtist(newArtist: ArtistPresentation) {
        artist = newArtist.toArtist()
    }

    fun updateArtists(newArtists: List<Artist>) {
        artists = newArtists
    }

    fun updateSearchArtists(newArtistItems: List<ContentItem.ArtistItem>) {
        artists = newArtistItems.map { it.toArtist() }
    }

    fun updateArtistResponseList(newArtistItems: List<ArtistPresentation>) {
        artists = newArtistItems.map { it.toArtist() }
    }

    fun getAllArtists(): List<Artist> {
        return artists
    }

    fun getArtistById(artistId: Long): Artist? {
        return artists.find { it.id == artistId }
    }

    fun clearAllArtists() {
        artists = emptyList()
    }
}