package vn.edu.usth.msma.utils.helpers

import vn.edu.usth.msma.data.Playlist
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.management.PlaylistResponse

fun ContentItem.PlaylistItem.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        playlistName = playlistName,
        playTimeLength = playTimeLength,
        releaseDate = releaseDate,
        songIdList = songIdList,
        songNameList = songNameList,
        artistIdList = artistIdList,
        artistNameList = artistNameList,
        imageUrl = imageUrl,
        description = description,
        status = status
    )
}

fun PlaylistResponse.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        playlistName = playlistName,
        playTimeLength = playTimeLength,
        releaseDate = releaseDate,
        songIdList = songIdList,
        songNameList = songNameList,
        artistIdList = artistIdList,
        artistNameList = artistNameList,
        imageUrl = imageUrl,
        description = description,
        status = status
    )
}
