package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.SongStatus;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.SongResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.LikeFollowingDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeFollowingDownloadServiceImpl implements LikeFollowingDownloadService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ArtistUserFollowRepository artistUserFollowRepository;
    private final SongRepository songRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final JwtHelper jwtHelper;
    private final UserSongDownloadRepository userSongDownloadRepository;

    private SongResponse convertToSongResponse(Song song) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

        String formattedDate = song.getReleaseDate() != null
                ? formatter.format(song.getReleaseDate())
                : null;

        List<String> genreNames = song.getGenreSongs() != null
                ? song.getGenreSongs().stream()
                .map(gs -> gs.getGenreSongId().getGenre().getGenresName())
                .filter(Objects::nonNull)
                .toList()
                : new ArrayList<>();

        List<String> artistNameList = song.getArtistSongs() != null
                ? song.getArtistSongs().stream()
                .map(as -> as.getArtistSongId().getArtist().getArtistName())
                .toList()
                : new ArrayList<>();

        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle() != null ? song.getTitle() : "")
                .releaseDate(formattedDate)
                .lyrics(song.getLyrics() != null ? song.getLyrics() : "")
                .duration(song.getDuration() != null ? song.getDuration() : "")
                .imageUrl(song.getImageUrl() != null ? song.getImageUrl() : "")
                .downloadPermission(song.getDownloadPermission() != null ? song.getDownloadPermission() : false)
                .description(song.getDescription() != null ? song.getDescription() : "")
                .mp3Url(song.getMp3Url() != null ? song.getMp3Url() : "")
                .songStatus(song.getSongStatus() != null ? song.getSongStatus().name() : null)
                .genreNameList(genreNames)
                .artistNameList(artistNameList)
                .numberOfListeners(userSongCountRepository.countDistinctUsersBySongId(song.getId()))
                .countListen(userSongCountRepository.getTotalCountListenBySongId(song.getId()))
                .numberOfDownload(userSongDownloadRepository.countDistinctUsersBySongId(song.getId()))
                .numberOfUserLike(userSongLikeRepository.countDistinctUsersBySongId(song.getId()))
                .build();
    }

    @Override
    public ApiResponse userFollowingArtist(Long artistId) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();
        
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
                .followedAt(now)
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

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    String formattedCreatedDate = artist.getCreatedDate() != null
                            ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                            : null;
                    String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                            ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                            : null;

                    return ArtistPresentation.builder()
                            .id(artist.getId())
                            .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                            .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                            .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                            .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                            .email(artist.getEmail())
                            .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                            .birthDay(formattedDate)
                            .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                            .status(artist.getStatus())
                            .createdBy(artist.getCreatedBy())
                            .lastModifiedBy(artist.getLastModifiedBy())
                            .createdDate(formattedCreatedDate)
                            .lastModifiedDate(formattedLastModifiedDate)
                            .description(artist.getDescription() != null ? artist.getDescription() : "")
                            .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                            .numberOfFollowers(totalNumberOfUserFollowers(artist.getId()))
                            .userType(artist.getUserType())
                            .artistSongIds(
                                    artist.getArtistSongs().stream()
                                            .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                            .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                            .collect(Collectors.toList())
                            )
                            .artistSongNameList(
                                    artist.getArtistSongs().stream()
                                            .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                            .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                            .collect(Collectors.toList())
                            )
                            .artistPlaylistIds(
                                    artist.getArtistPlaylists().stream()
                                            .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                            .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                            .collect(Collectors.toList())
                            )
                            .artistPlaylistNameList(
                                    artist.getArtistPlaylists().stream()
                                            .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                            .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                            .collect(Collectors.toList())
                            )
                            .artistAlbumIds(
                                    artist.getArtistAlbums().stream()
                                            .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                            .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                            .collect(Collectors.toList())
                            )
                            .artistAlbumNameList(
                                    artist.getArtistAlbums().stream()
                                            .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                            .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                            .collect(Collectors.toList())
                            ).build();
                })
                .toList();
    }

    @Override
    public Boolean isFollowedArtist(Long id) {
        Long userId = jwtHelper.getIdUserRequesting();
        return artistUserFollowRepository.existsByUserIdAndArtistId(userId, id);
    }

    @Override
    public ApiResponse userLikeSong(Long songId) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();
        
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
                .likedAt(now)
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
    public Boolean isFavoriteSong(Long songId) {
        Long userId = jwtHelper.getIdUserRequesting();
        return userSongLikeRepository.existsByUserIdAndSongId(userId, songId);
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
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

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
                .downloadedAt(now)
                .build();
        userSongDownloadRepository.save(userSongDownload);

        String downloadUrl;
        if (song.getMp3Url() != null && !song.getMp3Url().isBlank()) {
            downloadUrl = song.getMp3Url();
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
                    } else {
                        return "";
                    }
                })
                .toList();
    }

    // Helpers
    public Long totalNumberOfListeners(Long artistId) {
        List<Song> songs = songRepository.findByArtistId(artistId).stream()
                .filter(song -> !song.getSongStatus().equals(SongStatus.ACCEPTED))
                .toList();
        if (songs.isEmpty()) {
            return 0L;
        }
        List<Long> songIds = songs.stream().map(Song::getId).collect(Collectors.toList());
        return userSongCountRepository.countDistinctListenersBySongIds(songIds);
    }
    public Long totalNumberOfUserFollowers(Long artistId ) {
        return artistUserFollowRepository.countDistinctUsersByArtistId(artistId);
    }
}
