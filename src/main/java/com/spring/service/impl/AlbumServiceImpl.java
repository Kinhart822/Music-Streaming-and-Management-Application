package com.spring.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spring.constants.ApiResponseCode;
import com.spring.constants.ManageProcess;
import com.spring.constants.PlaylistAndAlbumStatus;
import com.spring.constants.SongStatus;
import com.spring.dto.request.music.artist.AddSongRequest;
import com.spring.dto.request.music.artist.AlbumRequest;
import com.spring.dto.request.music.artist.RemoveSongRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAlbumRepository artistAlbumRepository;
    private final SongRepository songRepository;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;

    @Override
    public AlbumResponse createAlbum(AlbumRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Album album = new Album();
        album.setAlbumName(request.getName());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            album.setImageUrl(imageUrl);
        }

        album.setDescription(request.getDescription());
        album.setReleaseDate(new Date());
        album.setAlbumTimeLength(0.0F);
        album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT);

        Album savedAlbum = albumRepository.save(album);

        ArtistAlbum artistAlbum = new ArtistAlbum();
        artistAlbum.setArtistAlbumId(new ArtistAlbumId(savedAlbum, artist));
        artistAlbumRepository.save(artistAlbum);

        return convertToResponse(album);
    }

    @Override
    public AlbumResponse updateAlbum(Long albumId, AlbumRequest request) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            album.setAlbumName(request.getName());
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            album.setDescription(request.getDescription());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            album.setImageUrl(cloudinaryService.uploadImageToCloudinary(request.getImage()));
        }

        return convertToResponse(albumRepository.save(album));

    }

    @Override
    public ApiResponse deleteAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (album.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PUBLIC){
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
            album.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.PUBLIC);
            albumRepository.save(album);
            return ApiResponse.ok("Album accepted successfully!");
        } else if (manageProcess.equalsIgnoreCase(ManageProcess.DECLINED.name())) {
            return ApiResponse.ok("Album declined! Please check your album before uploading again.");
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }
    }

    private AlbumResponse convertToResponse(Album album) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = album.getReleaseDate() != null ? sdf.format(album.getReleaseDate()) : null;

        return AlbumResponse.builder()
                .id(album.getId())
                .name(album.getAlbumName())
                .description(album.getDescription())
                .releaseDate(formattedDate)
                .imageUrl(album.getImageUrl())
                .albumTimeLength(album.getAlbumTimeLength())
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
}
