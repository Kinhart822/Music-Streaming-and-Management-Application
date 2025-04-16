package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.LikeFollowingDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeFollowingDownloadServiceImpl implements LikeFollowingDownloadService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ArtistUserFollowRepository artistUserFollowRepository;
    private final SongRepository songRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final JwtHelper jwtHelper;
    private final UserSongDownloadRepository userSongDownloadRepository;

    private SongResponse convertToSongResponse(Song song) {
        String formattedDate = song.getReleaseDate() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(song.getReleaseDate())
                : null;

        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle() != null ? song.getTitle() : "")
                .releaseDate(formattedDate)
                .lyrics(song.getLyrics() != null ? song.getLyrics() : "")
                .duration(song.getDuration() != null ? song.getDuration() : "")
                .imageUrl(song.getImageUrl() != null ? song.getImageUrl() : "")
                .artSmallUrl(song.getArtSmallUrl() != null ? song.getArtSmallUrl() : "")
                .artMediumUrl(song.getArtMediumUrl() != null ? song.getArtMediumUrl() : "")
                .artBigUrl(song.getArtBigUrl() != null ? song.getArtBigUrl() : "")
                .downloadPermission(song.getDownloadPermission() != null ? song.getDownloadPermission() : false)
                .description(song.getDescription() != null ? song.getDescription() : "")
                .mp3Url(song.getMp3Url() != null ? song.getMp3Url() : "")
                .trackUrl(song.getTrackUrl() != null ? song.getTrackUrl() : "")
                .songStatus(song.getSongStatus() != null ? song.getSongStatus().name() : null)
                .build();
    }

    @Override
    public ApiResponse userFollowingArtist(Long artistId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        ArtistUserFollowId id = new ArtistUserFollowId(artist, user);
        if (artistUserFollowRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
        }

        ArtistUserFollow saved = ArtistUserFollow.builder()
                .artistUserFollowId(id)
                .followedAt(Instant.now())
                .build();
        artistUserFollowRepository.save(saved);

        String fullName = artist.getLastName() + " " + artist.getFirstName();
        return ApiResponse.ok(String.format("Đã follow Artist %s!", fullName));
    }

    @Override
    public ApiResponse userUnfollowingArtist(Long artistId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        ArtistUserFollowId id = new ArtistUserFollowId(artist, user);
        if (!artistUserFollowRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }
        artistUserFollowRepository.deleteById(id);

        String fullName = artist.getLastName() + " " + artist.getFirstName();
        return ApiResponse.ok(String.format("Đã unfollow Artist %s!", fullName));
    }

    @Override
    public List<ArtistPresentation> getCurrentUserFollowedArtists() {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        List<Artist> artists = artistRepository.findByUser(user);
        if (artists.isEmpty()) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }

        return artists.stream()
                .map(artist -> {
                    String formattedDate = artist.getBirthDay() != null
                            ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : null;

                    return ArtistPresentation.builder()
                            .id(artist.getId())
                            .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                            .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                            .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                            .email(artist.getEmail())
                            .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                            .birthDay(formattedDate)
                            .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                            .status(artist.getStatus())
                            .createdBy(artist.getCreatedBy())
                            .lastModifiedBy(artist.getLastModifiedBy())
                            .createdDate(artist.getCreatedDate())
                            .lastModifiedDate(artist.getLastModifiedDate())
                            .description(artist.getDescription() != null ? artist.getDescription() : "")
                            .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                            .countListen(artist.getCountListen() != null ? artist.getCountListen() : 0)
                            .build();
                })
                .toList();
    }

    @Override
    public ApiResponse userLikeSong(Long songId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        UserSongLikeId likeId = new UserSongLikeId(song, user);
        if (userSongLikeRepository.existsById(likeId)) {
            throw new RuntimeException("Song already liked");
        }

        UserSongLike userSongLike = UserSongLike.builder()
                .userSongLikeId(likeId)
                .likedAt(Instant.now())
                .build();
        userSongLikeRepository.save(userSongLike);

        return ApiResponse.ok("User đã like song thành công");
    }

    @Override
    public ApiResponse userUnlikeSong(Long songId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSongLikeId id = new UserSongLikeId(song, user);
        if (!userSongLikeRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }
        userSongLikeRepository.deleteById(id);

        return ApiResponse.ok(String.format("Đã unlike Song %s!", song.getTitle()));
    }

    @Override
    public List<SongResponse> getCurrentUserLikedSongs() {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        List<Song> songs = songRepository.findByUser(user);
        if (songs.isEmpty()) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }

        return songs.stream()
                .map(this::convertToSongResponse)
                .toList();
    }

    @Override
    public ApiResponse userDownloadSong(Long songId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        UserSongDownloadId downloadId = new UserSongDownloadId(song, user);
        if (userSongDownloadRepository.existsById(downloadId)) {
            throw new RuntimeException("Song already downloaded");
        }

        UserSongDownload userSongDownload = UserSongDownload.builder()
                .userSongDownloadId(downloadId)
                .downloadedAt(Instant.now())
                .build();
        userSongDownloadRepository.save(userSongDownload);

        String downloadUrl;
        if (song.getMp3Url() != null && !song.getMp3Url().isBlank()) {
            downloadUrl = song.getMp3Url();
        } else if (song.getTrackUrl() != null && !song.getTrackUrl().isBlank()) {
            downloadUrl = song.getTrackUrl();
        } else {
            throw new BusinessException(ApiResponseCode.BAD_REQUEST);
        }

        return ApiResponse.ok(downloadUrl);
    }

    @Override
    public ApiResponse userUndownloadSong(Long songId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSongDownloadId downloadId = new UserSongDownloadId(song, user);
        if (!userSongDownloadRepository.existsById(downloadId)) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }
        userSongDownloadRepository.deleteById(downloadId);

        return ApiResponse.ok(String.format("Đã undownload Song %s!", song.getTitle()));
    }

    @Override
    public List<String> getUserDownloadedSongs() {
        Long userId = jwtHelper.getIdUserRequesting();
        List<UserSongDownload> downloadedSongs = userSongDownloadRepository.getAllUserDownload(userId);
        if (downloadedSongs.isEmpty()) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }

        return downloadedSongs.stream()
                .map(usd -> {
                    Song song = usd.getUserSongDownloadId().getSong();
                    if (song.getMp3Url() != null && !song.getMp3Url().isBlank()) {
                        return song.getMp3Url();
                    } else if (song.getTrackUrl() != null && !song.getTrackUrl().isBlank()) {
                        return song.getTrackUrl();
                    } else {
                        return "";
                    }
                })
                .toList();
    }
}
