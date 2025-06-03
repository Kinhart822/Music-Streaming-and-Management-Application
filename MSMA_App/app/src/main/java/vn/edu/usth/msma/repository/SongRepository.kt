package vn.edu.usth.msma.repository

import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.management.HistoryListenResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.utils.helpers.toSong

class SongRepository {
    private var songs: List<Song> = emptyList()

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
    }

    fun updateSearchSongs(newSongItems: List<ContentItem.SongItem>) {
        songs = newSongItems.map { it.toSong() }
    }

    fun updateSongResponseList(newSongItems: List<SongResponse>) {
        songs = newSongItems.map { it.toSong() }
    }

    fun updateHistoryListenResponseList(newSongItems: List<HistoryListenResponse>) {
        songs = newSongItems.map { it.toSong() }
    }

    fun getAllSongs(): List<Song> {
        return songs
    }

    fun getSongById(songId: Long): Song? {
        return songs.find { it.id == songId }
    }

    fun getNextSong(currentSongId: Long?): Song? {
        if (songs.isEmpty()) return null

        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        return if (currentIndex != -1 && currentIndex < songs.size - 1) {
            songs[currentIndex + 1]
        } else {
            songs.firstOrNull()
        }
    }

    fun getPreviousSong(currentSongId: Long?): Song? {
        if (songs.isEmpty()) return null

        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        return if (currentIndex != -1 && currentIndex > 0) {
            songs[currentIndex - 1]
        } else {
            songs.lastOrNull()
        }
    }

    fun playRandomSong(): Song? {
        return songs.randomOrNull()
    }

    fun clearAllSongs() {
        songs = emptyList()
    }
}