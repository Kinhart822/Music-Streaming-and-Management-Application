package com.spring.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spring.constants.ApiResponseCode;
import com.spring.constants.PlaylistAndAlbumStatus;
import com.spring.constants.SongStatus;
import com.spring.dto.request.music.artist.AddSongRequest;
import com.spring.dto.request.music.artist.PlaylistRequest;
import com.spring.dto.request.music.artist.RemoveSongRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {
    private final PlayListRepository playlistRepository;
    private final ArtistRepository artistRepository;
    private final ArtistPlaylistRepository artistPlaylistRepository;
    private final SongRepository songRepository;
    private final JwtHelper jwtHelper;
    private final Cloudinary cloudinary;
    private final UserRepository userRepository;

    private String uploadToCloudinary(MultipartFile file, String resourceType, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename();
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", resourceType,
                        "public_id", folder + "/" + UUID.randomUUID() + "_" + originalFilename
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    private String imageUpload(MultipartFile imageUpload) {
        try {
            if (imageUpload != null && !imageUpload.isEmpty()) {
                String imageOriginalFilename = imageUpload.getOriginalFilename();
                List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff");

                boolean isValidImage = allowedExtensions.stream()
                        .anyMatch(ext -> imageOriginalFilename.toLowerCase().endsWith(ext));

                if (!isValidImage) {
                    throw new IllegalArgumentException("Ảnh phải là một trong các định dạng: jpg, jpeg, png, gif, webp, bmp, tiff!");
                }

                return uploadToCloudinary(imageUpload, "image", "covers");
            }

            return null; // if imageUpload is null or empty
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage(), e);
        }
    }

    @Override
    public PlaylistResponse createPlaylist(PlaylistRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Playlist playlist = new Playlist();
        playlist.setPlaylistName(request.getPlaylistName());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = imageUpload(request.getImage());
            playlist.setImageUrl(imageUrl);
        }

        playlist.setDescription(request.getDescription());
        playlist.setPlaylistTimeLength(0f);
        playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.DRAFT);

        Playlist saved = playlistRepository.save(playlist);

        ArtistPlaylist artistPlaylist = new ArtistPlaylist();
        artistPlaylist.setArtistPlaylistId(new ArtistPlaylistId(artist, saved));
        artistPlaylistRepository.save(artistPlaylist);

        return convertToResponse(saved);
    }

    @Override
    public PlaylistResponse updatePlaylist(Long playlistId, PlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + playlistId));

        if (request.getPlaylistName() != null && !request.getPlaylistName().isBlank()) {
            playlist.setPlaylistName(request.getPlaylistName());
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            playlist.setDescription(request.getDescription());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            playlist.setImageUrl(imageUpload(request.getImage()));
        }

        return convertToResponse(playlistRepository.save(playlist));
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
        return convertToResponse(playlist);
    }

    @Override
    public List<PlaylistResponse> getAllPlaylistsByCurrentAccount() {
        Long id = jwtHelper.getIdUserRequesting();
        String role = jwtHelper.getUserRoleRequesting(); // Ví dụ: "USER", "ARTIST", "ADMIN"

        if ("USER".equalsIgnoreCase(role)) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            return user.getUserPlaylists().stream()
                    .map(up -> convertToResponse(up.getUserPlaylistId().getPlaylist()))
                    .collect(Collectors.toList());

        } else if ("ARTIST".equalsIgnoreCase(role)) {
            Artist artist = artistRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            return artist.getArtistPlaylists().stream()
                    .map(ap -> convertToResponse(ap.getArtistPlaylistId().getPlaylist()))
                    .collect(Collectors.toList());

        } else if ("ADMIN".equalsIgnoreCase(role)) {
            // Admin có thể xem toàn bộ playlist chẳng hạn (nếu có logic như vậy)
            List<Playlist> allPlaylists = playlistRepository.findAll();
            return allPlaylists.stream()
                    .filter(p -> p.getPlaylistAndAlbumStatus().equals(PlaylistAndAlbumStatus.PUBLIC))
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        throw new BusinessException(ApiResponseCode.INVALID_TYPE);
    }

    @Override
    public List<PlaylistResponse> getAllPlaylistsByArtistId(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return artist.getArtistPlaylists().stream()
                .map(ap -> convertToResponse(ap.getArtistPlaylistId().getPlaylist()))
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse addSongToPlaylist(AddSongRequest request) {
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
            PlaylistSongId id = new PlaylistSongId(song, playlist);
            PlaylistSong playlistSong = new PlaylistSong();
            playlistSong.setPlaylistSongId(id);

            // Thêm vào danh sách
            playlist.getPlaylistSongs().add(playlistSong);

            // Cộng thêm thời lượng bài hát vào playlist
            Float songDuration = convertDurationToFloat(song.getDuration()); // convert "mm:ss" or "hh:mm:ss" to float minutes
            Float currentLength = playlist.getPlaylistTimeLength() != null ? playlist.getPlaylistTimeLength() : 0.0F;
            playlist.setPlaylistTimeLength(currentLength + songDuration);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song added to playlist");
    }

    @Override
    public ApiResponse addListSongToPlaylist(AddSongRequest addSongRequest) {
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
                    PlaylistSongId id = new PlaylistSongId(song, playlist);
                    PlaylistSong playlistSong = new PlaylistSong();
                    playlistSong.setPlaylistSongId(id);
                    playlist.getPlaylistSongs().add(playlistSong);
                    currentLength += convertDurationToFloat(song.getDuration());
                } else {
                    throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
                }
            }

            playlist.setPlaylistTimeLength(currentLength);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs added to playlist");
    }

    @Override
    public ApiResponse removeSongFromPlaylist(RemoveSongRequest removeSongRequest) {
        Playlist playlist = playlistRepository.findById(removeSongRequest.getPlaylistId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT || playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.PENDING) {
            Song song = songRepository.findById(removeSongRequest.getSongId())
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

            PlaylistSongId id = new PlaylistSongId(song, playlist);

            playlist.getPlaylistSongs().removeIf(ps -> ps.getPlaylistSongId().equals(id));

            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Song removed from playlist");
    }

    @Override
    public ApiResponse removeListSongFromPlaylist(RemoveSongRequest removeSongRequest) {
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
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Songs removed from playlist");
    }

    @Override
    public ApiResponse uploadPlaylist(Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() == PlaylistAndAlbumStatus.DRAFT) {
            playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.PENDING);
            playlistRepository.save(playlist);
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        return ApiResponse.ok("Playlist uploaded successfully!");
    }

    private PlaylistResponse convertToResponse(Playlist playlist) {
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getPlaylistName())
                .imageUrl(playlist.getImageUrl())
                .description(playlist.getDescription())
                .playTimelength(playlist.getPlaylistTimeLength())
                .status(playlist.getPlaylistAndAlbumStatus().toString())
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
