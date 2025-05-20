package com.spring.service.impl;

import com.spring.constants.*;
import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.AdminAddAlbumRequest;
import com.spring.dto.request.music.AlbumRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.AlbumResponse;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.AlbumService;
import com.spring.service.CloudinaryService;
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
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;
    private final AlbumSongRepository albumSongRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAlbumRepository artistAlbumRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;
    private final UserSavedAlbumRepository userSavedAlbumRepository;

    @Override
    public AlbumResponse createAlbum(AlbumRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Long artistId = jwtHelper.getIdUserRequesting();
        Float totalLength = 0.0F;

        Album.AlbumBuilder albumBuilder = Album.builder()
                .albumName(request.getAlbumName())
                .description(request.getDescription())
                .albumTimeLength(totalLength)
                .playlistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT)
                .releaseDate(now)
                .createdDate(now)
                .lastModifiedDate(now);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            albumBuilder.imageUrl(imageUrl);
        }

        Album album = albumBuilder.build();
        albumRepository.save(album);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        artistAlbumRepository.save(new ArtistAlbum(new ArtistAlbumId(album, artist)));

        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            for (Long songId : request.getSongIds()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                albumSongRepository.save(new AlbumSong(new AlbumSongId(album, song)));
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

                artistAlbumRepository.save(new ArtistAlbum(new ArtistAlbumId(album, additionalArtist)));
            }
        }

        album.setAlbumTimeLength(totalLength);
        album.setLastModifiedDate(now);
        albumRepository.save(album);

        return convertToAlbumResponse(album);
    }

    @Override
    public AlbumResponse updateAlbum(Long albumId, AlbumRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Long artistId = jwtHelper.getIdUserRequesting();
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (request.getAlbumName() != null && !request.getAlbumName().isBlank()) {
            album.setAlbumName(request.getAlbumName());
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            album.setDescription(request.getDescription());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            album.setImageUrl(cloudinaryService.uploadImageToCloudinary(request.getImage()));
        }

        if (request.getSongIds() != null) {
            List<Long> newSongIds = request.getSongIds();
            List<Long> currentSongIds = album.getAlbumSongs().stream()
                    .map(ps -> ps.getAlbumSongId().getSong().getId())
                    .toList();

            for (Long songId : newSongIds) {
                if (!currentSongIds.contains(songId)) {
                    Song song = songRepository.findById(songId)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                    if (song.getSongStatus() == SongStatus.PENDING) {
                        throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                    }

                    albumSongRepository.save(new AlbumSong(new AlbumSongId(album, song)));
                }
            }

            album.getAlbumSongs().removeIf(ps -> !newSongIds.contains(ps.getAlbumSongId().getSong().getId()));
        } else {
            album.getAlbumSongs().clear();
        }

        Float totalLength = album.getAlbumSongs().stream()
                .map(ps -> convertDurationToFloat(ps.getAlbumSongId().getSong().getDuration()))
                .reduce(0.0F, Float::sum);
        album.setAlbumTimeLength(totalLength);

        if (request.getArtistIds() != null) {
            List<Long> newArtistIds = request.getArtistIds().stream()
                    .filter(id -> !id.equals(artistId))
                    .toList();

            List<Long> currentArtistIds = album.getArtistAlbums().stream()
                    .map(ap -> ap.getArtistAlbumId().getArtist().getId())
                    .filter(id -> !id.equals(artistId))
                    .toList();

            for (Long additionalArtistId : newArtistIds) {
                if (!currentArtistIds.contains(additionalArtistId)) {
                    Artist artist = artistRepository.findById(additionalArtistId)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                    if (artist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                        throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                    }

                    artistAlbumRepository.save(new ArtistAlbum(new ArtistAlbumId(album, artist)));
                }
            }

            album.getArtistAlbums().removeIf(ap -> {
                Long id = ap.getArtistAlbumId().getArtist().getId();
                return !id.equals(artistId) && !newArtistIds.contains(id);
            });
        } else {
            album.getArtistAlbums().removeIf(ap -> {
                Long id = ap.getArtistAlbumId().getArtist().getId();
                return !id.equals(artistId);
            });
        }

        album.setLastModifiedDate(now);
        album.setPlaylistAndAlbumStatus(album.getPlaylistAndAlbumStatus());
        Album updated = albumRepository.save(album);
        return convertToAlbumResponse(updated);
    }

    @Override
    public ApiResponse deleteAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        albumRepository.delete(album);
        return ApiResponse.ok("Album deleted successfully");
    }

    @Override
    public AlbumResponse getAlbumById(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        return convertToAlbumResponse(album);
    }

    @Override
    public List<AlbumResponse> getAllAlbumsByCurrentAccount() {
        Long id = jwtHelper.getIdUserRequesting();
        String role = jwtHelper.getUserRoleRequesting(); // Ví dụ: "USER", "ARTIST", "ADMIN"

        if ("ARTIST".equalsIgnoreCase(role)) {
            Artist artist = artistRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            return artist.getArtistAlbums().stream()
                    .map(ap -> convertToAlbumResponse(ap.getArtistAlbumId().getAlbum()))
                    .collect(Collectors.toList());

        } else if ("ADMIN".equalsIgnoreCase(role)) {
            List<Album> allAlbumList = albumRepository.findAll();
            return allAlbumList.stream()
                    .map(this::convertToAlbumResponse)
                    .collect(Collectors.toList());
        }

        throw new BusinessException(ApiResponseCode.INVALID_TYPE);
    }

    @Override
    public List<AlbumResponse> getAllAlbumsByArtistId(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return artist.getArtistAlbums().stream()
                .map(ap -> convertToAlbumResponse(ap.getArtistAlbumId().getAlbum()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AlbumResponse> getAllAcceptedAlbumsByArtistId(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return artist.getArtistAlbums().stream()
                .filter(p -> p.getArtistAlbumId().getAlbum().getPlaylistAndAlbumStatus().equals(PlaylistAndAlbumStatus.ACCEPTED))
                .map(ap -> convertToAlbumResponse(ap.getArtistAlbumId().getAlbum()))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean isSavedAlbum(Long id) {
        Long userId = jwtHelper.getIdUserRequesting();
        return userSavedAlbumRepository.existsByUserIdAndAlbumId(userId, id);
    }

    @Override
    public List<AlbumResponse> getCurrentUserSavedAlbums() {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        List<Album> albums = albumRepository.findByUser(user);
        if (albums.isEmpty()) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }

        return albums.stream()
                .map(this::convertToAlbumResponse)
                .toList();
    }

    @Override
    public List<AlbumResponse> getRecentCurrentUserSavedAlbums() {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        List<Album> albums = albumRepository.findByUser(user);
        if (albums.isEmpty()) {
            albums = albumRepository.findAll();
            return albums.stream()
                    .map(this::convertToAlbumResponse)
                    .sorted((p1, p2) -> p2.getReleaseDate().compareTo(p1.getReleaseDate()))
                    .limit(2)
                    .toList();
        }

        return albums.stream()
                .map(this::convertToAlbumResponse)
                .sorted((p1, p2) -> p2.getReleaseDate().compareTo(p1.getReleaseDate()))
                .limit(2)
                .toList();
    }

    @Override
    public ApiResponse addSongToAlbum(AddSongRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(request.getAlbumId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Song song = songRepository.findById(request.getSongId())
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            if (song.getSongStatus() == SongStatus.PENDING) {
                throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
            }

            // Kiểm tra đã tồn tại trong Album chưa
            boolean exists = album.getAlbumSongs().stream()
                    .anyMatch(ps -> ps.getAlbumSongId().getSong().getId().equals(song.getId()));

            if (exists) {
                throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
            }

            // Tạo AlbumSongId và AlbumSong
            AlbumSongId id = new AlbumSongId(album, song);
            AlbumSong albumSong = new AlbumSong();
            albumSong.setAlbumSongId(id);

            // Thêm vào danh sách
            album.getAlbumSongs().add(albumSong);

            // Cộng thêm thời lượng bài hát vào playlist
            Float songDuration = convertDurationToFloat(song.getDuration()); // convert "mm:ss" or "hh:mm:ss" to float minutes
            Float currentLength = album.getAlbumTimeLength() != null ? album.getAlbumTimeLength() : 0.0F;
            album.setAlbumTimeLength(currentLength + songDuration);
            album.setLastModifiedDate(now);
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song added to album");
    }

    @Override
    public ApiResponse addListSongToAlbum(AddSongRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(request.getAlbumId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Float currentLength = album.getAlbumTimeLength() != null ? album.getAlbumTimeLength() : 0.0F;

            for (Long songId : request.getSongIdList()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                boolean exists = album.getAlbumSongs().stream()
                        .anyMatch(ps -> ps.getAlbumSongId().getSong().getId().equals(songId));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                if (!exists) {
                    AlbumSongId id = new AlbumSongId(album, song);
                    AlbumSong albumSong = new AlbumSong();
                    albumSong.setAlbumSongId(id);
                    album.getAlbumSongs().add(albumSong);
                    currentLength += convertDurationToFloat(song.getDuration());
                } else {
                    throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
                }
            }

            album.setAlbumTimeLength(currentLength);
            album.setLastModifiedDate(now);
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs added to album");
    }

    @Override
    public ApiResponse removeSongFromAlbum(RemoveSongRequest removeSongRequest) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(removeSongRequest.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Song song = songRepository.findById(removeSongRequest.getSongId())
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            AlbumSongId id = new AlbumSongId(album, song);
            album.getAlbumSongs().removeIf(ps -> ps.getAlbumSongId().equals(id));
            album.setLastModifiedDate(now);
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song removed from album");
    }

    @Override
    public ApiResponse removeListSongFromAlbum(RemoveSongRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(request.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            List<Long> songIdsToRemove = request.getSongIdList();

            boolean anyRemoved = false;
            Iterator<AlbumSong> iterator = album.getAlbumSongs().iterator();

            Float newLength = 0.0F;

            while (iterator.hasNext()) {
                AlbumSong ps = iterator.next();
                Long songId = ps.getAlbumSongId().getSong().getId();
                if (songIdsToRemove.contains(songId)) {
                    iterator.remove(); // Loại bỏ bài hát
                    anyRemoved = true;
                } else {
                    newLength += convertDurationToFloat(ps.getAlbumSongId().getSong().getDuration());
                }
            }

            if (!anyRemoved) {
                throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
            }

            album.setAlbumTimeLength(newLength);
            album.setLastModifiedDate(now);
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs removed from album");
    }

    @Override
    public ApiResponse uploadAlbum(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT) {
            album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.PENDING);
            album.setLastModifiedDate(now);
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Playlist uploaded successfully!");
    }

    @Override
    public ApiResponse publishAlbum(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.ACCEPTED);
        album.setLastModifiedDate(now);
        albumRepository.save(album);
        return ApiResponse.ok("Playlist accepted!");
    }

    @Override
    public ApiResponse declineAlbum(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.DECLINED);
        album.setLastModifiedDate(now);
        albumRepository.save(album);
        return ApiResponse.ok("Playlist declined!");
    }

    @Override
    public ApiResponse adminAddAlbumRequest(AdminAddAlbumRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();
        Float totalLength = 0.0F;

        Album.AlbumBuilder albumBuilder = Album.builder()
                .albumName(request.getAlbumName())
                .description(request.getDescription())
                .albumTimeLength(totalLength)
                .playlistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT)
                .releaseDate(now)
                .createdDate(now)
                .lastModifiedDate(now);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            albumBuilder.imageUrl(imageUrl);
        }

        Album album = albumBuilder.build();
        albumRepository.save(album);

        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            for (Long songId : request.getSongIds()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                albumSongRepository.save(new AlbumSong(new AlbumSongId(album, song)));
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

                artistAlbumRepository.save(new ArtistAlbum(new ArtistAlbumId(album, artist)));
            }
        }

        album.setAlbumTimeLength(totalLength);
        album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.ACCEPTED);
        album.setLastModifiedDate(now);
        albumRepository.save(album);

        return ApiResponse.ok("Thêm album thành công!");
    }

    @Override
    public ApiResponse userSaveAlbum(Long albumId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        // Tạo ID tổng hợp
        UserSavedAlbumId id = new UserSavedAlbumId(album, user);

        // Kiểm tra đã lưu chưa (nếu muốn tránh duplicate)
        if (userSavedAlbumRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
        }

        // Tạo và lưu entity
        UserSavedAlbum saved = UserSavedAlbum.builder()
                .userSavedAlbumId(id)
                .build();

        userSavedAlbumRepository.save(saved);

        return ApiResponse.ok("Album đã được lưu!");
    }

    @Override
    public ApiResponse userUnSaveAlbum(Long albumId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSavedAlbumId id = new UserSavedAlbumId(album, user);
        if (!userSavedAlbumRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND);
        }
        userSavedAlbumRepository.deleteById(id);
        return ApiResponse.ok("Đã unsaved Playlist !");
    }

    private AlbumResponse convertToAlbumResponse(Album album) {
        Long id = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = album.getReleaseDate() != null
                ? formatter.format(album.getReleaseDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        List<Long> songIdList = album.getAlbumSongs() != null
                ? album.getAlbumSongs().stream()
                .filter(Objects::nonNull)
                .map(albumSong -> albumSong.getAlbumSongId().getSong().getId())
                .toList()
                : Collections.emptyList();
        List<String> songNameList = album.getAlbumSongs() != null
                ? album.getAlbumSongs().stream()
                .filter(Objects::nonNull)
                .map(albumSong -> albumSong.getAlbumSongId().getSong().getTitle())
                .toList()
                : Collections.emptyList();

        if (user.getUserType() == UserType.ARTIST) {
            List<Long> additionalArtistIdList = Optional.ofNullable(
                            artistAlbumRepository.findByArtistAlbumId_Album_Id(album.getId()))
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(ap -> !ap.getArtistAlbumId().getArtist().getId().equals(id))
                    .map(ap -> ap.getArtistAlbumId().getArtist().getId())
                    .toList();
            List<String> additionalArtistList = Optional.ofNullable(
                            artistAlbumRepository.findByArtistAlbumId_Album_Id(album.getId()))
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(ap -> !ap.getArtistAlbumId().getArtist().getId().equals(id))
                    .map(ap -> ap.getArtistAlbumId().getArtist().getArtistName())
                    .toList();

            return AlbumResponse.builder()
                    .id(album.getId())
                    .albumName(album.getAlbumName())
                    .description(album.getDescription())
                    .albumTimeLength(album.getAlbumTimeLength())
                    .releaseDate(formattedDate)
                    .songIdList(songIdList)
                    .songNameList(songNameList)
                    .artistIdList(additionalArtistIdList)
                    .artistNameList(additionalArtistList)
                    .imageUrl(album.getImageUrl())
                    .status(album.getPlaylistAndAlbumStatus().toString())
                    .build();
        } else {
            List<Long> artistIdList = album.getArtistAlbums() != null
                    ? album.getArtistAlbums().stream()
                    .map(as -> as.getArtistAlbumId().getArtist().getId())
                    .toList()
                    : new ArrayList<>();
            List<String> artistNameList = album.getArtistAlbums() != null
                    ? album.getArtistAlbums().stream()
                    .map(as -> as.getArtistAlbumId().getArtist().getArtistName())
                    .toList()
                    : new ArrayList<>();

            return AlbumResponse.builder()
                    .id(album.getId())
                    .albumName(album.getAlbumName())
                    .description(album.getDescription())
                    .albumTimeLength(album.getAlbumTimeLength())
                    .releaseDate(formattedDate)
                    .songIdList(artistIdList)
                    .songNameList(songNameList)
                    .artistIdList(artistIdList)
                    .artistNameList(artistNameList)
                    .imageUrl(album.getImageUrl())
                    .status(album.getPlaylistAndAlbumStatus().toString())
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
    public Long totalAlbums() {
        return albumRepository.countAllAlbums();
    }

    @Override
    public Long totalAlbumsByArtist() {
        Long artistId = jwtHelper.getIdUserRequesting();
        return albumRepository.findByArtistId(artistId).stream().count();
    }

    @Override
    public Long totalPendingAlbums() {
        return albumRepository.countAllPendingAlbums();
    }
}
