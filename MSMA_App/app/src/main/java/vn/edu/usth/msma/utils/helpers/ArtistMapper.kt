package vn.edu.usth.msma.utils.helpers

import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.profile.ArtistPresentation

fun ContentItem.ArtistItem.toArtist(): Artist {
    return Artist(
        id = id,
        avatar = avatar,
        firstName = firstName,
        lastName = lastName,
        artistName = artistName,
        description = description,
        image = image,
        numberOfFollowers = numberOfFollowers,
        email = email,
        gender = gender,
        birthDay = birthDay,
        phone = phone,
        status = status,
        createdBy = createdBy,
        lastModifiedBy = lastModifiedBy,
        createdDate = createdDate,
        lastModifiedDate = lastModifiedDate,
        userType = userType,
        artistSongIds = artistSongIds,
        artistSongNameList = artistSongNameList,
        artistPlaylistIds = artistPlaylistIds,
        artistPlaylistNameList = artistPlaylistNameList,
        artistAlbumIds = artistAlbumIds,
        artistAlbumNameList = artistAlbumNameList
    )
}

fun ArtistPresentation.toArtist(): Artist {
    return Artist(
        id = id,
        avatar = avatar,
        firstName = firstName,
        lastName = lastName,
        artistName = artistName,
        description = description,
        image = image,
        numberOfFollowers = numberOfFollowers,
        email = email,
        gender = gender,
        birthDay = birthDay,
        phone = phone,
        status = status,
        createdBy = createdBy,
        lastModifiedBy = lastModifiedBy,
        createdDate = createdDate,
        lastModifiedDate = lastModifiedDate,
        userType = userType,
        artistSongIds = artistSongIds,
        artistSongNameList = artistSongNameList,
        artistPlaylistIds = artistPlaylistIds,
        artistPlaylistNameList = artistPlaylistNameList,
        artistAlbumIds = artistAlbumIds,
        artistAlbumNameList = artistAlbumNameList
    )
}
