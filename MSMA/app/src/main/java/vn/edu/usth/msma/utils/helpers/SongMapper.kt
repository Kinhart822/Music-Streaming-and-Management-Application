package vn.edu.usth.msma.utils.helpers

import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.ui.screen.search.songs.Song

fun ContentItem.SongItem.toSong(): Song {
    return Song(
        id = id,
        title = title,
        releaseDate = releaseDate,
        lyrics = lyrics,
        duration = duration,
        imageUrl = imageUrl,
        downloadPermission = downloadPermission,
        description = description,
        mp3Url = mp3Url,
        songStatus = songStatus,
        genreNameList = genreNameList,
        artistNameList = artistNameList,
        numberOfListeners = numberOfListeners,
        countListen = countListen,
        numberOfUserLike = numberOfUserLike,
        numberOfDownload = numberOfDownload
    )
}
