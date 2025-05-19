package vn.edu.usth.msma.utils.helpers

import vn.edu.usth.msma.data.Album
import vn.edu.usth.msma.data.dto.response.management.AlbumResponse
import vn.edu.usth.msma.data.dto.response.management.ContentItem

fun ContentItem.AlbumItem.toAlbum(): Album {
    return Album(
        id = id,
        albumName = albumName,
        description = description,
        releaseDate = releaseDate,
        albumTimeLength = albumTimeLength,
        songIdList = songIdList,
        songNameList = songNameList,
        artistIdList = artistIdList,
        artistNameList = artistNameList,
        imageUrl = imageUrl,
        status = status
    )
}

fun AlbumResponse.toAlbum(): Album {
    return Album(
        id = id,
        albumName = albumName,
        description = description,
        releaseDate = releaseDate,
        albumTimeLength = albumTimeLength,
        songIdList = songIdList,
        songNameList = songNameList,
        artistIdList = artistIdList,
        artistNameList = artistNameList,
        imageUrl = imageUrl,
        status = status
    )
}
