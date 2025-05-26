package com.spring.service.impl;

import com.spring.constants.*;
import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.AdminAddPlaylistRequest;
import com.spring.dto.request.music.PlaylistRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.CloudinaryService;
import com.spring.service.NotificationService;
import com.spring.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final UserSavedPlaylistRepository userSavedPlaylistRepository;
    private final ArtistRepository artistRepository;
    private final ArtistPlaylistRepository artistPlaylistRepository;
    private final ArtistUserFollowRepository artistUserFollowRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;

    @Override
    public PlaylistResponse createPlaylist(PlaylistRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Long artistId = jwtHelper.getIdUserRequesting();
        Float totalLength = 0.0F;

        Playlist.PlaylistBuilder playlistBuilder = Playlist.builder()
                .playlistName(request.getPlaylistName())
                .description(request.getDescription())
                .playlistTimeLength(totalLength)
                .playlistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT)
                .releaseDate(now)
                .createdDate(now)
                .lastModifiedDate(now);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            playlistBuilder.imageUrl(imageUrl);
        }

        Playlist playlist = playlistBuilder.build();
        playlistRepository.save(playlist);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        artistPlaylistRepository.save(new ArtistPlaylist(new ArtistPlaylistId(playlist, artist)));

        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            for (Long songId : request.getSongIds()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                playlistSongRepository.save(new PlaylistSong(new PlaylistSongId(playlist, song)));
                totalLength += convertDurationToFloat(song.getDuration());
            }
        }

        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            for (Long additionalArtistId : request.getArtistIds()) {
                Artist additionalArtist = artistRepository.findById(additionalArtistId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (additionalArtist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                    throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                }

                artistPlaylistRepository.save(new ArtistPlaylist(new ArtistPlaylistId(playlist, additionalArtist)));
            }
        }

        playlist.setPlaylistTimeLength(totalLength);
        playlist.setLastModifiedDate(now);
        playlistRepository.save(playlist);

        return convertToPlaylistResponse(playlist);
    }

    @Override
    public PlaylistResponse updatePlaylist(Long playlistId, PlaylistRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Long artistId = jwtHelper.getIdUserRequesting();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (request.getPlaylistName() != null && !request.getPlaylistName().isBlank()) {
            playlist.setPlaylistName(request.getPlaylistName());
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            playlist.setDescription(request.getDescription());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            playlist.setImageUrl(cloudinaryService.uploadImageToCloudinary(request.getImage()));
        }

        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            List<Long> newSongIds = request.getSongIds();
            List<Long> currentSongIds = playlist.getPlaylistSongs().stream()
                    .map(ps -> ps.getPlaylistSongId().getSong().getId())
                    .toList();

            for (Long songId : newSongIds) {
                if (!currentSongIds.contains(songId)) {
                    Song song = songRepository.findById(songId)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                    if (song.getSongStatus() == SongStatus.PENDING) {
                        throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                    }

                    playlistSongRepository.save(new PlaylistSong(new PlaylistSongId(playlist, song)));
                }
            }

            playlist.getPlaylistSongs().removeIf(ps -> !newSongIds.contains(ps.getPlaylistSongId().getSong().getId()));
        } else {
            playlist.getPlaylistSongs().clear();
        }

        Float totalLength = playlist.getPlaylistSongs().stream()
                .map(ps -> convertDurationToFloat(ps.getPlaylistSongId().getSong().getDuration()))
                .reduce(0.0F, Float::sum);
        playlist.setPlaylistTimeLength(totalLength);

        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            List<Long> newArtistIds = request.getArtistIds().stream()
                    .filter(id -> !id.equals(artistId))
                    .toList();

            List<Long> currentArtistIds = playlist.getArtistPlaylists().stream()
                    .map(ap -> ap.getArtistPlaylistId().getArtist().getId())
                    .filter(id -> !id.equals(artistId))
                    .toList();

            for (Long additionalArtistId : newArtistIds) {
                if (!currentArtistIds.contains(additionalArtistId)) {
                    Artist artist = artistRepository.findById(additionalArtistId)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                    if (artist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                        throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                    }

                    artistPlaylistRepository.save(new ArtistPlaylist(new ArtistPlaylistId(playlist, artist)));
                }
            }

            playlist.getArtistPlaylists().removeIf(ap -> {
                Long id = ap.getArtistPlaylistId().getArtist().getId();
                return !id.equals(artistId) && !newArtistIds.contains(id);
            });
        } else {
            playlist.getArtistPlaylists().removeIf(ap -> {
                Long id = ap.getArtistPlaylistId().getArtist().getId();
                return !id.equals(artistId);
            });
        }

        playlist.setLastModifiedDate(now);
        playlist.setPlaylistAndAlbumStatus(playlist.getPlaylistAndAlbumStatus());
        Playlist updated = playlistRepository.save(playlist);
        return convertToPlaylistResponse(updated);
    }

    @Override
    public ApiResponse deletePlaylist(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        playlistRepository.delete(playlist);
        return ApiResponse.ok("Playlist deleted successfully");
    }

    @Override
    public PlaylistResponse getPlaylistById(Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        return convertToPlaylistResponse(playlist);
    }

    @Override
    public List<PlaylistResponse> getAllPlaylistsByCurrentAccount() {
        Long id = jwtHelper.getIdUserRequesting();
        String role = jwtHelper.getUserRoleRequesting(); // Ví dụ: "USER", "ARTIST", "ADMIN"

        if ("USER".equalsIgnoreCase(role)) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            return user.getUserSavedPlaylists().stream()
                    .map(up -> convertToPlaylistResponse(up.getUserSavedPlaylistId().getPlaylist()))
                    .collect(Collectors.toList());

        } else if ("ARTIST".equalsIgnoreCase(role)) {
            Artist artist = artistRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            return artist.getArtistPlaylists().stream()
                    .map(ap -> convertToPlaylistResponse(ap.getArtistPlaylistId().getPlaylist()))
                    .collect(Collectors.toList());

        } else if ("ADMIN".equalsIgnoreCase(role)) {
            List<Playlist> allPlaylists = playlistRepository.findAll();
            return allPlaylists.stream()
                    .filter(p -> p.getPlaylistAndAlbumStatus().equals(PlaylistAndAlbumStatus.ACCEPTED))
                    .map(this::convertToPlaylistResponse)
                    .collect(Collectors.toList());
        }

        throw new BusinessException(ApiResponseCode.INVALID_TYPE);
    }

    @Override
    public List<PlaylistResponse> getAllPlaylistsByArtistId(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return artist.getArtistPlaylists().stream()
                .map(ap -> convertToPlaylistResponse(ap.getArtistPlaylistId().getPlaylist()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PlaylistResponse> getAllAcceptedPlaylistsByArtistId(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return artist.getArtistPlaylists().stream()
                .filter(p -> p.getArtistPlaylistId().getPlaylist().getPlaylistAndAlbumStatus().equals(PlaylistAndAlbumStatus.ACCEPTED))
                .map(ap -> convertToPlaylistResponse(ap.getArtistPlaylistId().getPlaylist()))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean isSavedPlaylist(Long id) {
        Long userId = jwtHelper.getIdUserRequesting();
        return userSavedPlaylistRepository.existsByUserIdAndPlaylistId(userId, id);
    }

    @Override
    public List<PlaylistResponse> getCurrentUserSavedPlaylists() {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        List<Playlist> playlists = playlistRepository.findByUser(user);
        if (playlists.isEmpty()) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }

        return playlists.stream()
                .map(this::convertToPlaylistResponse)
                .toList();
    }

    @Override
    public List<PlaylistResponse> getRecentCurrentUserSavedPlaylists() {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        List<Playlist> playlists = playlistRepository.findByUser(user);
        if (playlists.isEmpty()) {
            playlists = playlistRepository.findAll();
            return playlists.stream()
                    .map(this::convertToPlaylistResponse)
                    .sorted((p1, p2) -> p2.getReleaseDate().compareTo(p1.getReleaseDate()))
                    .limit(2)
                    .toList();
        }

        return playlists.stream()
                .map(this::convertToPlaylistResponse)
                .sorted((p1, p2) -> p2.getReleaseDate().compareTo(p1.getReleaseDate()))
                .limit(2)
                .toList();
    }

    @Override
    public ApiResponse addSongToPlaylist(AddSongRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(request.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Song song = songRepository.findById(request.getSongId())
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            if (song.getSongStatus() == SongStatus.PENDING) {
                throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
            }

            // Kiểm tra đã tồn tại trong playlist chưa
            boolean exists = playlist.getPlaylistSongs().stream()
                    .anyMatch(ps -> ps.getPlaylistSongId().getSong().getId().equals(song.getId()));

            if (exists) {
                throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
            }

            // Tạo PlaylistSongId và PlaylistSong
            PlaylistSongId id = new PlaylistSongId(playlist, song);
            PlaylistSong playlistSong = new PlaylistSong();
            playlistSong.setPlaylistSongId(id);
            playlist.getPlaylistSongs().add(playlistSong);

            // Cộng thêm thời lượng bài hát vào playlist
            Float songDuration = convertDurationToFloat(song.getDuration()); // convert "mm:ss" or "hh:mm:ss" to float minutes
            Float currentLength = playlist.getPlaylistTimeLength() != null ? playlist.getPlaylistTimeLength() : 0.0F;
            playlist.setPlaylistTimeLength(currentLength + songDuration);
            playlist.setLastModifiedDate(now);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song added to playlist");
    }

    @Override
    public ApiResponse addListSongToPlaylist(AddSongRequest addSongRequest) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(addSongRequest.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Float currentLength = playlist.getPlaylistTimeLength() != null ? playlist.getPlaylistTimeLength() : 0.0F;

            for (Long songId : addSongRequest.getSongIdList()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                boolean exists = playlist.getPlaylistSongs().stream()
                        .anyMatch(ps -> ps.getPlaylistSongId().getSong().getId().equals(songId));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                if (!exists) {
                    PlaylistSongId id = new PlaylistSongId(playlist, song);
                    PlaylistSong playlistSong = new PlaylistSong();
                    playlistSong.setPlaylistSongId(id);
                    playlist.getPlaylistSongs().add(playlistSong);
                    currentLength += convertDurationToFloat(song.getDuration());
                } else {
                    throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
                }
            }

            playlist.setPlaylistTimeLength(currentLength);
            playlist.setLastModifiedDate(now);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs added to playlist");
    }

    @Override
    public ApiResponse removeSongFromPlaylist(RemoveSongRequest removeSongRequest) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(removeSongRequest.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Song song = songRepository.findById(removeSongRequest.getSongId())
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            PlaylistSongId id = new PlaylistSongId(playlist, song);

            playlist.getPlaylistSongs().removeIf(ps -> ps.getPlaylistSongId().equals(id));

            playlist.setLastModifiedDate(now);

            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song removed from playlist");
    }

    @Override
    public ApiResponse removeListSongFromPlaylist(RemoveSongRequest removeSongRequest) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(removeSongRequest.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            List<Long> songIdsToRemove = removeSongRequest.getSongIdList();

            boolean anyRemoved = false;
            Iterator<PlaylistSong> iterator = playlist.getPlaylistSongs().iterator();

            Float newLength = 0.0F;

            while (iterator.hasNext()) {
                PlaylistSong ps = iterator.next();
                Long songId = ps.getPlaylistSongId().getSong().getId();
                if (songIdsToRemove.contains(songId)) {
                    iterator.remove(); // Loại bỏ bài hát
                    anyRemoved = true;
                } else {
                    newLength += convertDurationToFloat(ps.getPlaylistSongId().getSong().getDuration());
                }
            }

            if (!anyRemoved) {
                throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
            }

            playlist.setPlaylistTimeLength(newLength);
            playlist.setLastModifiedDate(now);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs removed from playlist");
    }

    @Override
    public ApiResponse uploadPlaylist(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT) {
            playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.PENDING);
            playlist.setLastModifiedDate(now);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Playlist uploaded successfully!");
    }

    @Override
    public ApiResponse publishPlaylist(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.ACCEPTED);
        playlist.setLastModifiedDate(now);
        playlistRepository.save(playlist);
        for (ArtistPlaylist artistPlaylist : playlist.getArtistPlaylists()) {
            Long artistId = artistPlaylist.getArtistPlaylistId().getArtist().getId();
            notificationService.notifyArtistPlaylistAccepted(artistId, playlist.getPlaylistName());
            for (Long userId : artistUserFollowRepository.findByArtistId(artistId)) {
                notificationService.notifyUserNewPlaylist(userId, artistId, playlist.getPlaylistName());
            }
        }
        return ApiResponse.ok("Playlist accepted!");
    }

    @Override
    public ApiResponse declinePlaylist(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.DECLINED);
        playlist.setLastModifiedDate(now);
        playlistRepository.save(playlist);
        for (ArtistPlaylist artistPlaylist : playlist.getArtistPlaylists()) {
            Long artistId = artistPlaylist.getArtistPlaylistId().getArtist().getId();
            notificationService.notifyArtistPlaylistDeclined(artistId, playlist.getPlaylistName());
        }
        return ApiResponse.ok("Playlist declined!");
    }

    @Override
    public ApiResponse adminAddPlaylistRequest(AdminAddPlaylistRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();
        Float totalLength = 0.0F;

        Playlist.PlaylistBuilder playlistBuilder = Playlist.builder()
                .playlistName(request.getPlaylistName())
                .description(request.getDescription())
                .playlistTimeLength(totalLength)
                .playlistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT)
                .releaseDate(now)
                .createdDate(now)
                .lastModifiedDate(now);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            playlistBuilder.imageUrl(imageUrl);
        }

        Playlist playlist = playlistBuilder.build();
        playlistRepository.save(playlist);

        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            for (Long songId : request.getSongIds()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                playlistSongRepository.save(new PlaylistSong(new PlaylistSongId(playlist, song)));
                totalLength += convertDurationToFloat(song.getDuration());
            }
        }

        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            for (Long artistId : request.getArtistIds()) {
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (artist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                    throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                }

                artistPlaylistRepository.save(new ArtistPlaylist(new ArtistPlaylistId(playlist, artist)));
            }
        }

        playlist.setPlaylistTimeLength(totalLength);
        playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.ACCEPTED);
        playlist.setLastModifiedDate(now);
        playlistRepository.save(playlist);

        return ApiResponse.ok("Thêm playlist thành công!");
    }

    @Override
    public ApiResponse userSavePlaylist(Long playlistId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSavedPlaylistId id = new UserSavedPlaylistId(playlist, user);

        if (userSavedPlaylistRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
        }

        UserSavedPlaylist saved = UserSavedPlaylist.builder()
                .userSavedPlaylistId(id)
                .build();

        userSavedPlaylistRepository.save(saved);

        return ApiResponse.ok("Playlist đã được lưu!");
    }

    @Override
    public ApiResponse userUnSavePlaylist(Long playlistId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSavedPlaylistId id = new UserSavedPlaylistId(playlist, user);
        if (!userSavedPlaylistRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }
        userSavedPlaylistRepository.deleteById(id);
        return ApiResponse.ok("Đã unsaved Playlist !");
    }

    private PlaylistResponse convertToPlaylistResponse(Playlist playlist) {
        Long id = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = playlist.getReleaseDate() != null
                ? formatter.format(playlist.getReleaseDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        List<Long> songIdList = playlist.getPlaylistSongs() != null
                ? playlist.getPlaylistSongs().stream()
                .filter(Objects::nonNull)
                .map(playlistSong -> playlistSong.getPlaylistSongId().getSong().getId())
                .toList()
                : Collections.emptyList();
        List<String> songNameList = playlist.getPlaylistSongs() != null
                ? playlist.getPlaylistSongs().stream()
                .filter(Objects::nonNull)
                .map(playlistSong -> playlistSong.getPlaylistSongId().getSong().getTitle())
                .toList()
                : Collections.emptyList();

        if (user.getUserType() == UserType.ARTIST) {
            List<Long> additionalArtistIdList = Optional.ofNullable(
                            artistPlaylistRepository.findByArtistPlaylistId_Playlist_Id(playlist.getId()))
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(ap -> !ap.getArtistPlaylistId().getArtist().getId().equals(id))
                    .map(ap -> ap.getArtistPlaylistId().getArtist().getId())
                    .toList();
            List<String> additionalArtistNameList = Optional.ofNullable(
                            artistPlaylistRepository.findByArtistPlaylistId_Playlist_Id(playlist.getId()))
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(ap -> !ap.getArtistPlaylistId().getArtist().getId().equals(id))
                    .map(ap -> ap.getArtistPlaylistId().getArtist().getArtistName())
                    .toList();

            return PlaylistResponse.builder()
                    .id(playlist.getId())
                    .playlistName(playlist.getPlaylistName())
                    .description(playlist.getDescription())
                    .playTimeLength(playlist.getPlaylistTimeLength())
                    .releaseDate(formattedDate)
                    .songIdList(songIdList)
                    .songNameList(songNameList)
                    .artistIdList(additionalArtistIdList)
                    .artistNameList(additionalArtistNameList)
                    .imageUrl(playlist.getImageUrl())
                    .status(playlist.getPlaylistAndAlbumStatus().toString())
                    .build();
        } else {
            List<Long> artistIdList = playlist.getArtistPlaylists() != null
                    ? playlist.getArtistPlaylists().stream()
                    .map(as -> as.getArtistPlaylistId().getArtist().getId())
                    .toList()
                    : new ArrayList<>();
            List<String> artistNameList = playlist.getArtistPlaylists() != null
                    ? playlist.getArtistPlaylists().stream()
                    .map(as -> as.getArtistPlaylistId().getArtist().getArtistName())
                    .toList()
                    : new ArrayList<>();

            return PlaylistResponse.builder()
                    .id(playlist.getId())
                    .playlistName(playlist.getPlaylistName())
                    .description(playlist.getDescription())
                    .playTimeLength(playlist.getPlaylistTimeLength())
                    .releaseDate(formattedDate)
                    .songIdList(songIdList)
                    .songNameList(songNameList)
                    .artistIdList(artistIdList)
                    .artistNameList(artistNameList)
                    .imageUrl(playlist.getImageUrl())
                    .status(playlist.getPlaylistAndAlbumStatus().toString())
                    .build();
        }
    }

    private Float convertDurationToFloat(String duration) {
        if (duration == null || duration.isBlank()) return 0.0F;

        String[] parts = duration.split(":");

        int hours = 0, minutes = 0, seconds = 0;

        if (parts.length == 3) {
            hours = Integer.parseInt(parts[0]);
            minutes = Integer.parseInt(parts[1]);
            seconds = Integer.parseInt(parts[2]);
        } else if (parts.length == 2) {
            minutes = Integer.parseInt(parts[0]);
            seconds = Integer.parseInt(parts[1]);
        }

        return (float) (hours * 3600 + minutes * 60 + seconds);
    }

    @Override
    public Long totalPlaylists() {
        return playlistRepository.countAllPlaylists();
    }

    @Override
    public Long totalPendingPlaylists() {
        return playlistRepository.countAllPendingPlaylists();
    }

    @Override
    public Long totalArtistPlaylists() {
        Long artistId = jwtHelper.getIdUserRequesting();
        return (long) playlistRepository.findByArtistId(artistId).size();
    }
}
