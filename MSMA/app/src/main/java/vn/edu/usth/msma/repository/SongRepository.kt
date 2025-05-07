package vn.edu.usth.msma.repository

import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.ui.screen.search.songs.Song
import vn.edu.usth.msma.utils.helpers.toSong

class SongRepository {
    private var songItems: List<ContentItem.SongItem> = emptyList()

    fun updateSongs(newSongItems: List<ContentItem.SongItem>) {
        songItems = newSongItems
    }

    fun getSongs(): List<Song> {
        return songItems.map { it.toSong() }
    }

    fun getNextSong(currentSongId: Long?): Song? {
        val songs = getSongs()
        if (songs.isEmpty()) return null

        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        return if (currentIndex != -1 && currentIndex < songs.size - 1) {
            songs[currentIndex + 1]
        } else {
            songs.firstOrNull()
        }
    }

    fun getPreviousSong(currentSongId: Long?): Song? {
        val songs = getSongs()
        if (songs.isEmpty()) return null

        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        return if (currentIndex != -1 && currentIndex > 0) {
            songs[currentIndex - 1]
        } else {
            songs.lastOrNull()
        }
    }

    fun playRandomSong(): Song? {
        val songs = getSongs()
        return songs.randomOrNull()
    }

    fun getSongById(songId: Long): Song? {
        return getSongs().find { it.id == songId }
    }

    // Favorite Songs (Call API and DataStore) - Save/Remove/Check/Get

}