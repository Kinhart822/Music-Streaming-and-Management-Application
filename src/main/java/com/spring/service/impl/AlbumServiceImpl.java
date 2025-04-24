package com.spring.service.impl;

import com.spring.constants.*;
import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.AlbumRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.AlbumResponse;
import com.spring.dto.response.ApiResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAlbumRepository artistAlbumRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;
    private final UserSavedAlbumRepository userSavedAlbumRepository;

    @Override
    public AlbumResponse createAlbum(AlbumRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Album album = new Album();
        album.setAlbumName(request.getAlbumName());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            album.setImageUrl(imageUrl);
        }

        Float totalLength = 0.0F;

        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            for (Long songId : request.getSongIds()) {
                Song song = songRepository.findById(songId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (song.getSongStatus() == SongStatus.PENDING) {
                    throw new BusinessException(ApiResponseCode.INVALID_SONG_STATUS);
                }

                AlbumSong albumSong = new AlbumSong();
                albumSong.setAlbumSongId(new AlbumSongId(song, album));
                album.getAlbumSongs().add(albumSong);
                totalLength += convertDurationToFloat(song.getDuration());
            }
        }

        if (request.getAdditionalArtistIds() != null && !request.getAdditionalArtistIds().isEmpty()) {
            for (Long additionalArtistId : request.getAdditionalArtistIds()) {
                Artist additionalArtist = artistRepository.findById(additionalArtistId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (additionalArtist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                    throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                }

                ArtistAlbum additionalArtistAlbum = new ArtistAlbum();
                additionalArtistAlbum.setArtistAlbumId(new ArtistAlbumId(album, additionalArtist));
                album.getArtistAlbums().add(additionalArtistAlbum);
            }
        }

        album.setDescription(request.getDescription());
        album.setAlbumTimeLength(totalLength);
        album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT);
        album.setReleaseDate(Instant.now());
        album.setCreatedDate(Instant.now());
        album.setLastModifiedDate(Instant.now());

        Album savedAlbum = albumRepository.save(album);

        ArtistAlbum artistAlbum = new ArtistAlbum();
        artistAlbum.setArtistAlbumId(new ArtistAlbumId(savedAlbum, artist));
        artistAlbumRepository.save(artistAlbum);

        return convertToResponse(album);
    }

    @Override
    public AlbumResponse updateAlbum(Long albumId, AlbumRequest request) {
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

                    AlbumSong newAlbumSong = new AlbumSong();
                    newAlbumSong.setAlbumSongId(new AlbumSongId(song, album));
                    album.getAlbumSongs().add(newAlbumSong);
                }
            }

            album.getAlbumSongs().removeIf(ps -> !newSongIds.contains(ps.getAlbumSongId().getSong().getId()));
        }

        Float totalLength = album.getAlbumSongs().stream()
                .map(ps -> convertDurationToFloat(ps.getAlbumSongId().getSong().getDuration()))
                .reduce(0.0F, Float::sum);
        album.setAlbumTimeLength(totalLength);

        if (request.getAdditionalArtistIds() != null) {
            List<Long> newArtistIds = request.getAdditionalArtistIds().stream()
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

                    ArtistAlbum newArtistAlbum = new ArtistAlbum();
                    newArtistAlbum.setArtistAlbumId(new ArtistAlbumId(album, artist));
                    album.getArtistAlbums().add(newArtistAlbum);
                }
            }

            album.getArtistAlbums().removeIf(ap -> {
                Long id = ap.getArtistAlbumId().getArtist().getId();
                return !id.equals(artistId) && !newArtistIds.contains(id);
            });
        }

        album.setLastModifiedDate(Instant.now());
        if (album.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.DECLINED) {
            album.setPlaylistAndAlbumStatus(album.getPlaylistAndAlbumStatus());
        } else {
            album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.EDITED);
        }
        Album updated = albumRepository.save(album);
        return convertToResponse(updated);
    }

    @Override
    public ApiResponse deleteAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.ACCEPTED) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        albumRepository.delete(album);
        return ApiResponse.ok("Album deleted successfully");
    }

    @Override
    public AlbumResponse getAlbumById(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        return convertToResponse(album);
    }

    @Override
    public List<AlbumResponse> getAllAlbumsByCurrentAccount() {
        Long id = jwtHelper.getIdUserRequesting();
        String role = jwtHelper.getUserRoleRequesting(); // Ví dụ: "USER", "ARTIST", "ADMIN"

        if ("ARTIST".equalsIgnoreCase(role)) {
            Artist artist = artistRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            return artist.getArtistAlbums().stream()
                    .map(ap -> convertToResponse(ap.getArtistAlbumId().getAlbum()))
                    .collect(Collectors.toList());

        } else if ("ADMIN".equalsIgnoreCase(role)) {
            List<Album> allAlbumList = albumRepository.findAll();
            return allAlbumList.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        throw new BusinessException(ApiResponseCode.INVALID_TYPE);
    }

    @Override
    public List<AlbumResponse> getAllAlbumsByArtistId(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return artist.getArtistAlbums().stream()
                .map(ap -> convertToResponse(ap.getArtistAlbumId().getAlbum()))
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse addSongToAlbum(AddSongRequest request) {
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
            AlbumSongId id = new AlbumSongId(song, album);
            AlbumSong albumSong = new AlbumSong();
            albumSong.setAlbumSongId(id);

            // Thêm vào danh sách
            album.getAlbumSongs().add(albumSong);

            // Cộng thêm thời lượng bài hát vào playlist
            Float songDuration = convertDurationToFloat(song.getDuration()); // convert "mm:ss" or "hh:mm:ss" to float minutes
            Float currentLength = album.getAlbumTimeLength() != null ? album.getAlbumTimeLength() : 0.0F;
            album.setAlbumTimeLength(currentLength + songDuration);
            album.setLastModifiedDate(Instant.now());
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song added to album");
    }

    @Override
    public ApiResponse addListSongToAlbum(AddSongRequest request) {
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
                    AlbumSongId id = new AlbumSongId(song, album);
                    AlbumSong albumSong = new AlbumSong();
                    albumSong.setAlbumSongId(id);
                    album.getAlbumSongs().add(albumSong);
                    currentLength += convertDurationToFloat(song.getDuration());
                } else {
                    throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
                }
            }

            album.setAlbumTimeLength(currentLength);
            album.setLastModifiedDate(Instant.now());
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs added to album");
    }

    @Override
    public ApiResponse removeSongFromAlbum(RemoveSongRequest removeSongRequest) {
        Album album = albumRepository.findById(removeSongRequest.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Song song = songRepository.findById(removeSongRequest.getSongId())
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            AlbumSongId id = new AlbumSongId(song, album);

            album.getAlbumSongs().removeIf(ps -> ps.getAlbumSongId().equals(id));
            album.setLastModifiedDate(Instant.now());
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song removed from album");
    }

    @Override
    public ApiResponse removeListSongFromAlbum(RemoveSongRequest request) {
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
            album.setLastModifiedDate(Instant.now());
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs removed from album");
    }

    @Override
    public ApiResponse uploadAlbum(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT) {
            album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.PENDING);
            album.setLastModifiedDate(Instant.now());
            albumRepository.save(album);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Playlist uploaded successfully!");
    }

    @Override
    public ApiResponse manageUploadAlbum(Long id, String manageProcess) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        if (manageProcess.equalsIgnoreCase(ManageProcess.ACCEPTED.name())) {
            album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.ACCEPTED);
            album.setLastModifiedDate(Instant.now());
            albumRepository.save(album);
            return ApiResponse.ok("Album accepted successfully!");
        } else if (manageProcess.equalsIgnoreCase(ManageProcess.DECLINED.name())) {
            return ApiResponse.ok("Album declined! Please check your album before uploading again.");
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }
    }

    @Override
    public ApiResponse userSaveAlbum(Long albumId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        // Tạo ID tổng hợp
        UserSavedAlbumId id = new UserSavedAlbumId(user, album);

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

    private AlbumResponse convertToResponse(Album album) {
        Long id = jwtHelper.getIdUserRequesting();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = album.getReleaseDate() != null
                ? formatter.format(album.getReleaseDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        List<String> songNameList = album.getAlbumSongs() != null
                ? album.getAlbumSongs().stream()
                .filter(Objects::nonNull)
                .map(albumSong -> albumSong.getAlbumSongId().getSong().getTitle())
                .toList()
                : Collections.emptyList();

        List<String> additionalArtistList = Optional.ofNullable(
                        artistAlbumRepository.findByArtistAlbumId_Album_Id(album.getId()))
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(ap -> !ap.getArtistAlbumId().getArtist().getId().equals(id))
                .map(ap -> ap.getArtistAlbumId().getArtist().getFirstName() + " " +
                        ap.getArtistAlbumId().getArtist().getLastName())
                .toList();

        return AlbumResponse.builder()
                .id(album.getId())
                .albumName(album.getAlbumName())
                .description(album.getDescription())
                .albumTimeLength(album.getAlbumTimeLength())
                .releaseDate(formattedDate)
                .songNameList(songNameList)
                .additionalArtistNameList(additionalArtistList)
                .imageUrl(album.getImageUrl())
                .status(album.getPlaylistAndAlbumStatus().toString())
                .build();
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
    public Long totalAlbum() {
        Long artistId = jwtHelper.getIdUserRequesting();

        return albumRepository.findByArtistId(artistId).stream()
                .filter(album -> !album.getPlaylistAndAlbumStatus().equals(PlaylistAndAlbumStatus.ACCEPTED))
                .count();
    }
}
