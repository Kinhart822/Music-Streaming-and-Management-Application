package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.ManageProcess;
import com.spring.constants.PlaylistAndAlbumStatus;
import com.spring.constants.SongStatus;
import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.PlaylistRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.CloudinaryService;
import com.spring.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final UserSavedPlaylistRepository userSavedPlaylistRepository;
    private final ArtistRepository artistRepository;
    private final ArtistPlaylistRepository artistPlaylistRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;

    @Override
    public PlaylistResponse createPlaylist(PlaylistRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Playlist playlist = new Playlist();
        playlist.setPlaylistName(request.getPlaylistName());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
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

    @Override
    public ApiResponse manageUploadPlaylist(Long id, String manageProcess) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (playlist.getPlaylistAndAlbumStatus() != PlaylistAndAlbumStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        if (manageProcess.equalsIgnoreCase(ManageProcess.ACCEPTED.name())) {
            playlist.setPlaylistAndAlbumStatus(PlaylistAndAlbumStatus.PUBLIC);
            playlistRepository.save(playlist);
            return ApiResponse.ok("Playlist accepted successfully!");
        } else if (manageProcess.equalsIgnoreCase(ManageProcess.DECLINED.name())) {
            return ApiResponse.ok("Playlist declined! Please check your playlist before uploading again.");
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }
    }

    @Override
    public ApiResponse userSavePlaylist(Long playlistId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSavedPlaylistId id = new UserSavedPlaylistId(user, playlist);

        if (userSavedPlaylistRepository.existsById(id)) {
            throw new BusinessException(ApiResponseCode.ALREADY_EXISTS);
        }

        UserSavedPlaylist saved = UserSavedPlaylist.builder()
                .userSavedPlaylistId(id)
                .build();

        userSavedPlaylistRepository.save(saved);

        return ApiResponse.ok("Playlist đã được lưu!");
    }

    private PlaylistResponse convertToResponse(Playlist playlist) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = playlist.getReleaseDate() != null ? sdf.format(playlist.getReleaseDate()) : null;

        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getPlaylistName())
                .imageUrl(playlist.getImageUrl())
                .releaseDate(formattedDate)
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
