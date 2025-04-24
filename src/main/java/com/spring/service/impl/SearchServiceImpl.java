package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.UserType;
import com.spring.dto.request.PaginationAccountRequest;
import com.spring.dto.request.PaginationGenreRequest;
import com.spring.dto.request.PaginationPlaylistAlbumRequest;
import com.spring.dto.request.PaginationSongRequest;
import com.spring.dto.response.*;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playListRepository;
    private final AlbumRepository albumRepository;
    private final GenreRepository genreRepository;
    private final ArtistSongRepository artistSongRepository;
    private final ArtistPlaylistRepository artistPlaylistRepository;
    private final ArtistAlbumRepository artistAlbumRepository;
    private final UserSongDownloadRepository userSongDownloadRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final JwtHelper jwtHelper;

    @Override
    public Map<String, Object> paginationAccount(PaginationAccountRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                request.getOrder().equalsIgnoreCase("desc") ? Sort.by(request.getOrderBy()).descending() : Sort.by(request.getOrderBy()).ascending()
        );

        UserType userType = request.getUserType();
        Page<?> resultPage;

        if (userType == null) {
            throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }

        resultPage = switch (userType) {
            case USER ->
                    userRepository.searchByUserTypeAndStatusAndKeyword(UserType.USER, request.getStatus(), request.getSearch(), pageable)
                            .map(this::convertToUserPresentation);
            case ADMIN ->
                    userRepository.searchByUserTypeAndStatusAndKeyword(UserType.ADMIN, request.getStatus(), request.getSearch(), pageable)
                            .map(this::convertToAdminPresentation);
            case ARTIST ->
                    artistRepository.searchByUserTypeAndStatusAndKeyword(UserType.ARTIST, request.getStatus(), request.getSearch(), pageable)
                            .map(this::convertToArtistPresentation);
        };

        int currentPage = resultPage.getNumber() + 1;
        int totalPages = resultPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", resultPage.getContent());
        response.put("currentPage", currentPage);
        response.put("totalPages", totalPages);
        response.put("totalElements", resultPage.getTotalElements());

        return response;
    }

    @Override
    public Map<String, Object> paginationRecentSongs(PaginationSongRequest request) {
        String search = request.getSearch().trim().toLowerCase();
        Long genreId = request.getGenreId();

        Sort sort = Sort.by("releaseDate").descending();
        Pageable pageable = PageRequest.of(0, 10, sort);

        Page<Song> songsPage = songRepository.findSongsByFilter(
                pageable,
                search.isEmpty() ? null : search,
                genreId
        );

        List<SongResponse> songResponses = songsPage.getContent().stream()
                .map(this::convertToSongResponse)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("songs", songResponses);
        result.put("currentPage", 1);
        result.put("totalPages", 1);
        result.put("totalElements", songResponses.size());

        return result;
    }

    @Override
    public Map<String, Object> paginationSongs(PaginationSongRequest request) {
        String search = request.getSearch().trim().toLowerCase();
        Long genreId = request.getGenreId();
        String order = request.getOrder().toLowerCase();
        String orderBy = request.getOrderBy();

        Page<Song> songsPage;
        Pageable pageable;

        if (orderBy.equals("songCount")) {
            // Sắp xếp theo tổng count_listen
            pageable = PageRequest.of(request.getPage() - 1, request.getSize());

            songsPage = songRepository.findSongsOrderByListenCount(
                    pageable,
                    search.isEmpty() ? null : search,
                    genreId,
                    order
            );
        } else {
            // Mặc định sort theo releaseDate
            Sort sort = order.equals("asc")
                    ? Sort.by("releaseDate").ascending()
                    : Sort.by("releaseDate").descending();

            pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

            songsPage = songRepository.findSongsByFilter(
                    pageable,
                    search.isEmpty() ? null : search,
                    genreId
            );
        }

        List<SongResponse> songResponses = songsPage.getContent().stream()
                .map(this::convertToSongResponse)
                .toList();

        int currentPage = songsPage.getNumber() + 1;
        int totalPages = songsPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("songs", songResponses);
        result.put("currentPage", currentPage);
        result.put("totalPages", totalPages);
        result.put("totalElements", songsPage.getTotalElements());

        return result;
    }

    @Override
    public Map<String, Object> paginationPlaylists(PaginationPlaylistAlbumRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                request.getOrder().equalsIgnoreCase("desc") ?
                        Sort.by(request.getOrderBy()).descending() :
                        Sort.by(request.getOrderBy()).ascending()
        );

        Page<Playlist> playlistPage = playListRepository.searchByKeyword(request.getSearch(), pageable);
        List<PlaylistResponse> content = playlistPage
                .map(this::convertToPlaylistResponse)
                .getContent();

        int currentPage = playlistPage.getNumber() + 1;
        int totalPages = playlistPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", currentPage);
        response.put("totalPages", totalPages);
        response.put("totalElements", playlistPage.getTotalElements());

        return response;
    }

    @Override
    public Map<String, Object> paginationAlbums(PaginationPlaylistAlbumRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                request.getOrder().equalsIgnoreCase("desc") ?
                        Sort.by(request.getOrderBy()).descending() :
                        Sort.by(request.getOrderBy()).ascending()
        );

        Page<Album> albumPage = albumRepository.searchByKeyword(request.getSearch(), pageable);
        List<AlbumResponse> content = albumPage
                .map(this::convertToAlbumResponse)
                .getContent();

        int currentPage = albumPage.getNumber() + 1;
        int totalPages = albumPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", currentPage);
        response.put("totalPages", totalPages);
        response.put("totalElements", albumPage.getTotalElements());

        return response;
    }

    @Override
    public Map<String, Object> paginationGenres(PaginationGenreRequest request) {
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());

        Page<Genre> genrePage = genreRepository.searchByKeyword(request.getSearch(), pageable);

        int currentPage = genrePage.getNumber() + 1;
        int totalPages = genrePage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        List<GenreResponse> content = genrePage.map(this::convertToGenreResponse).getContent();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", currentPage);
        response.put("totalPages", totalPages);
        response.put("totalElements", genrePage.getTotalElements());

        return response;
    }

    // Helper Method
    private UserPresentation convertToUserPresentation(User user) {
        String formattedDate = user.getBirthDay() != null
                ? user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : null;

        return UserPresentation.builder()
                .id(user.getId())
                .avatar(user.getAvatar() != null ? user.getAvatar() : "")
                .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .email(user.getEmail())
                .gender(user.getGender() != null ? user.getGender().toString() : "")
                .birthDay(formattedDate)
                .phone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .lastModifiedBy(user.getLastModifiedBy())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }

    private AdminPresentation convertToAdminPresentation(User admin) {
        String formattedDate = admin.getBirthDay() != null
                ? admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : null;

        return AdminPresentation.builder()
                .id(admin.getId())
                .avatar(admin.getAvatar() != null ? admin.getAvatar() : "")
                .firstName(admin.getFirstName() != null ? admin.getFirstName() : "")
                .lastName(admin.getLastName() != null ? admin.getLastName() : "")
                .email(admin.getEmail())
                .gender(admin.getGender() != null ? admin.getGender().toString() : "")
                .birthDay(formattedDate)
                .phone(admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "")
                .status(admin.getStatus())
                .createdBy(admin.getCreatedBy())
                .lastModifiedBy(admin.getLastModifiedBy())
                .createdDate(admin.getCreatedDate())
                .lastModifiedDate(admin.getLastModifiedDate())
                .build();
    }

    private ArtistPresentation convertToArtistPresentation(Artist artist) {
        String formattedDate = artist.getBirthDay() != null
                ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
                .createdDate(artist.getCreatedDate())
                .lastModifiedDate(artist.getLastModifiedDate())
                .description(artist.getDescription() != null ? artist.getDescription() : "")
                .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                .countListen(artist.getCountListen() != null ? artist.getCountListen() : 0)
                .build();
    }

    private SongResponse convertToSongResponse(Song song) {
        Long id = jwtHelper.getIdUserRequesting();

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

        List<String> additionalArtistNameList = Optional.ofNullable(
                        artistSongRepository.findByArtistSongId_Song_Id(song.getId()))
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(ap -> !ap.getArtistSongId().getArtist().getId().equals(id))
                .map(ap -> ap.getArtistSongId().getArtist().getArtistName())
                .toList();

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
                .genreNameList(genreNames)
                .additionalArtistNameList(additionalArtistNameList)
                .numberOfListeners(userSongCountRepository.countDistinctUsersBySongId(song.getId()))
                .countListen(userSongCountRepository.getTotalCountListenBySongId(song.getId()))
                .numberOfDownload(userSongDownloadRepository.countDistinctUsersBySongId(song.getId()))
                .numberOfUserLike(userSongLikeRepository.countDistinctUsersBySongId(song.getId()))
                .build();
    }

    private PlaylistResponse convertToPlaylistResponse(Playlist playlist) {
        Long id = jwtHelper.getIdUserRequesting();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = playlist.getReleaseDate() != null
                ? formatter.format(playlist.getReleaseDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        List<String> songNameList = playlist.getPlaylistSongs() != null
                ? playlist.getPlaylistSongs().stream()
                .filter(Objects::nonNull)
                .map(playlistSong -> playlistSong.getPlaylistSongId().getSong().getTitle())
                .toList()
                : Collections.emptyList();

        List<String> additionalArtistList = Optional.ofNullable(
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
                .playTimelength(playlist.getPlaylistTimeLength())
                .releaseDate(formattedDate)
                .songNameList(songNameList)
                .additionalArtistNameList(additionalArtistList)
                .imageUrl(playlist.getImageUrl())
                .status(playlist.getPlaylistAndAlbumStatus().toString())
                .build();
    }

    private AlbumResponse convertToAlbumResponse(Album album) {
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
                .map(ap -> ap.getArtistAlbumId().getArtist().getArtistName())
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

    private GenreResponse convertToGenreResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .briefDescription(genre.getBriefDescription())
                .fullDescription(genre.getFullDescription())
                .build();
    }
}
