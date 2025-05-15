package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.UserType;
import com.spring.dto.request.*;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final ArtistSongRepository artistSongRepository;
    private final PlaylistRepository playListRepository;
    private final ArtistPlaylistRepository artistPlaylistRepository;
    private final AlbumRepository albumRepository;
    private final ArtistAlbumRepository artistAlbumRepository;
    private final GenreRepository genreRepository;
    private final UserSongDownloadRepository userSongDownloadRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final JwtHelper jwtHelper;

    @Override
    public Map<String, Object> paginationAccount(PaginationAccountRequest request) {
        Pageable pageable = PageRequest.of(
                Math.max(request.getPage() - 1, 0),
                request.getSize(),
                request.getOrder().equalsIgnoreCase("desc")
                        ? Sort.by(request.getOrderBy()).descending()
                        : Sort.by(request.getOrderBy()).ascending()
        );

        String search = request.getSearch();
        UserType userType = request.getParsedUserType();
        Integer status = request.getStatus();

        Page<User> userPage;

        if (userType != null && status != null) {
            userPage = userRepository.searchByUserTypeAndStatusAndKeyword(userType, status, search, pageable);
        } else if (userType != null) {
            userPage = userRepository.searchByUserTypeAndKeyword(userType, search, pageable);
        } else if (status != null) {
            userPage = userRepository.searchByStatusAndKeyword(status, search, pageable);
        } else {
            userPage = userRepository.searchByKeyword(search, pageable);
        }

        int currentPage = userPage.getNumber() + 1;
        int totalPages = userPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        List<Object> content = userPage.stream()
                .map(user -> {
                    if (user.getUserType() == UserType.ADMIN) {
                        return convertToAdminPresentation(user);
                    } else if (user.getUserType() == UserType.ARTIST) {
                        Artist artist = artistRepository.findById(user.getId())
                                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
                        return convertToArtistPresentation(artist);
                    } else {
                        return convertToUserPresentation(user);
                    }
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", currentPage);
        response.put("totalPages", totalPages);
        response.put("totalElements", userPage.getTotalElements());

        return response;
    }

    @Override
    public Map<String, Object> paginationRecentSongs(PaginationSongRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        String search = request.getSearch().trim().toLowerCase();
        Long genreId = request.getGenreId();

        Sort sort = Sort.by("releaseDate").descending();
        Pageable pageable = PageRequest.of(0, 10, sort);

        Page<Song> songsPage = songRepository.findArtistSongsByFilter(
                pageable,
                search.isEmpty() ? null : search,
                genreId,
                artistId
        );

        return buildSongsPaginationResult(songsPage);
    }

    @Override
    public Map<String, Object> paginationArtistSongs(PaginationSongRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        String search = request.getSearch().trim().toLowerCase();
        Long genreId = request.getGenreId();
        String orderBy = request.getOrderBy();
        String order = request.getOrder();

        Pageable pageable = createSongPageable(request.getPage(), request.getSize(), orderBy, order);
        Page<Song> songsPage;

        switch (orderBy) {
            case "numberOfListeners" -> {
                if (order.equalsIgnoreCase("asc")) {
                    songsPage = songRepository.findArtistSongsOrderByNumberOfListenersAsc(pageable, search.isEmpty() ? null : search, genreId, artistId);
                } else if (order.equalsIgnoreCase("desc")) {
                    songsPage = songRepository.findArtistSongsOrderByNumberOfListenersDesc(pageable, search.isEmpty() ? null : search, genreId, artistId);
                } else {
                    throw new BusinessException(ApiResponseCode.INVALID_FORMAT);
                }
            }
            case "countListen" -> {
                if (order.equalsIgnoreCase("asc")) {
                    songsPage = songRepository.findArtistSongsOrderByListenCountAsc(pageable, search.isEmpty() ? null : search, genreId, artistId);
                } else if (order.equalsIgnoreCase("desc")) {
                    songsPage = songRepository.findArtistSongsOrderByListenCountDesc(pageable, search.isEmpty() ? null : search, genreId, artistId);
                } else {
                    throw new BusinessException(ApiResponseCode.INVALID_FORMAT);
                }
            }
            default -> songsPage = songRepository.findArtistSongsByFilter(
                    pageable, search.isEmpty() ? null : search, genreId, artistId);
        }

        return buildSongsPaginationResult(songsPage);
    }

    @Override
    public Map<String, Object> paginationSongs(PaginationSongRequest request) {
        String search = request.getSearch().trim().toLowerCase();
        Long genreId = request.getGenreId();
        String order = request.getOrder();
        String orderBy = request.getOrderBy();

        Pageable pageable = createSongPageable(request.getPage(), request.getSize(), orderBy, order);
        Page<Song> songsPage;

        switch (orderBy) {
            case "numberOfListeners" -> {
                if (order.equalsIgnoreCase("asc")) {
                    songsPage = songRepository.findSongsOrderByNumberOfListenersAsc(pageable, search.isEmpty() ? null : search, genreId);
                } else if (order.equalsIgnoreCase("desc")) {
                    songsPage = songRepository.findSongsOrderByNumberOfListenersDesc(pageable, search.isEmpty() ? null : search, genreId);
                } else {
                    throw new BusinessException(ApiResponseCode.INVALID_FORMAT);
                }
            }
            case "countListen" -> {
                if (order.equalsIgnoreCase("asc")) {
                    songsPage = songRepository.findSongsOrderByListenCountAsc(pageable, search.isEmpty() ? null : search, genreId);
                } else if (order.equalsIgnoreCase("desc")) {
                    songsPage = songRepository.findSongsOrderByListenCountDesc(pageable, search.isEmpty() ? null : search, genreId);
                } else {
                    throw new BusinessException(ApiResponseCode.INVALID_FORMAT);
                }
            }
            default -> songsPage = songRepository.findSongsByFilter(
                    pageable, search.isEmpty() ? null : search, genreId);
        }

        return buildSongsPaginationResult(songsPage);
    }

    @Override
    public Map<String, Object> paginationArtistPlaylists(PaginationPlaylistRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        String search = request.getSearch().trim().toLowerCase();
        String orderBy = request.getOrderBy();
        String order = request.getOrder();

        Pageable pageable = createPlaylistPageable(request.getPage(), request.getSize(), orderBy, order);
        Page<Playlist> playlistPage = playListRepository.findArtistPlaylistsByFilter(pageable, search, artistId);

        return buildPlaylistPaginationResult(playlistPage);
    }

    @Override
    public Map<String, Object> paginationPlaylists(PaginationPlaylistRequest request) {
        String search = request.getSearch().trim().toLowerCase();
        String orderBy = request.getOrderBy();
        String order = request.getOrder();

        Pageable pageable = createPlaylistPageable(request.getPage(), request.getSize(), orderBy, order);
        Page<Playlist> playlistPage = playListRepository.findPlaylistsByFilter(pageable, search);

        return buildPlaylistPaginationResult(playlistPage);
    }

    @Override
    public Map<String, Object> paginationArtistAlbums(PaginationAlbumRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        String search = request.getSearch().trim().toLowerCase();
        String orderBy = request.getOrderBy();
        String order = request.getOrder();

        Pageable pageable = createAlbumPageable(request.getPage(), request.getSize(), orderBy, order);
        Page<Album> albumPage = albumRepository.findArtistAlbumsByFilter(pageable, search, artistId);

        return buildAlbumsPaginationResult(albumPage);
    }

    @Override
    public Map<String, Object> paginationAlbums(PaginationAlbumRequest request) {
        String search = request.getSearch().trim().toLowerCase();
        String orderBy = request.getOrderBy();
        String order = request.getOrder();

        Pageable pageable = createAlbumPageable(request.getPage(), request.getSize(), orderBy, order);
        Page<Album> albumPage = albumRepository.findAlbumsByFilter(pageable, search);

        return buildAlbumsPaginationResult(albumPage);
    }

    @Override
    public Map<String, Object> paginationGenres(PaginationGenreRequest request) {
        String search = request.getSearch() != null ? request.getSearch().trim().toLowerCase() : "";
        String orderBy = request.getOrderBy() != null ? request.getOrderBy() : "createdDate";
        String order = request.getOrder() != null ? request.getOrder() : "desc";

        Sort sort;
        switch (orderBy) {
            case "genresName" -> sort = order.equalsIgnoreCase("asc")
                    ? Sort.by("genresName").ascending()
                    : Sort.by("genresName").descending();
            case "createdDate" -> sort = order.equalsIgnoreCase("asc")
                    ? Sort.by("createdDate").ascending()
                    : Sort.by("createdDate").descending();
            default -> sort = Sort.by("createdDate").descending();
        }

        Pageable pageable = PageRequest.of(Math.max(request.getPage() - 1, 0), request.getSize(), sort);
        Page<Genre> genrePage = genreRepository.searchByKeyword(search, pageable);

        if (request.getPage() > genrePage.getTotalPages() && genrePage.getTotalPages() > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        List<GenreResponse> content = genrePage.stream()
                .map(this::convertToGenreResponse)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", genrePage.getNumber() + 1);
        response.put("totalPages", genrePage.getTotalPages());
        response.put("totalElements", genrePage.getTotalElements());

        return response;
    }

    @Override
    public Map<String, Object> paginationRecentContents(PaginationContentRequest request) {
        String search = request.getSearch().trim().toLowerCase();
        String orderBy = request.getOrderBy();
        String order = request.getOrder();
        int page = Math.max(request.getPage() - 1, 0); // Page bắt đầu từ 0
        int size = Math.min(request.getSize(), 10); // Giới hạn tối đa 10 bản ghi

        // Tạo Sort cho truy vấn
        Sort sort = switch (orderBy) {
            case "title" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("title").ascending() : Sort.by("title").descending();
            case "releaseDate" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("releaseDate").ascending() : Sort.by("releaseDate").descending();
            default -> Sort.by("releaseDate").descending();
        };

        // Truy vấn songs, playlists, albums với tìm kiếm, sắp xếp và giới hạn 10 bản ghi mỗi loại
        Pageable pageable = PageRequest.of(0, 10, sort);
        Page<Song> songPage = songRepository.findSongsByFilter(pageable, search.isEmpty() ? null : search, null);
        Page<Playlist> playlistPage = playListRepository.findPlaylistsByFilter(pageable, search);
        Page<Album> albumPage = albumRepository.findAlbumsByFilter(pageable, search);

        // Kết hợp danh sách songs, playlists, albums
        List<Object> combinedContent = new ArrayList<>();
        combinedContent.addAll(songPage.getContent());
        combinedContent.addAll(playlistPage.getContent());
        combinedContent.addAll(albumPage.getContent());

        // Sắp xếp lại danh sách kết hợp
        Comparator<Object> comparator;
        if (orderBy.equals("title")) {
            comparator = order.equalsIgnoreCase("asc")
                    ? Comparator.comparing(obj -> getTitle(obj), String.CASE_INSENSITIVE_ORDER)
                    : Comparator.comparing(obj -> getTitle(obj), String.CASE_INSENSITIVE_ORDER).reversed();
        } else {
            comparator = order.equalsIgnoreCase("asc")
                    ? Comparator.comparing(obj -> getReleaseDate(obj), Comparator.nullsLast(Comparator.naturalOrder()))
                    : Comparator.comparing(obj -> getReleaseDate(obj), Comparator.nullsLast(Comparator.reverseOrder()));
        }
        combinedContent.sort(comparator);

        // Giới hạn tổng số bản ghi là 10
        int totalElements = Math.min(combinedContent.size(), 10);
        combinedContent = combinedContent.subList(0, totalElements);

        // Áp dụng phân trang thủ công
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (page + 1 > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<Object> paginatedContent = combinedContent.subList(start, end).stream()
                .map(content -> {
                    if (content instanceof Song song) {
                        return convertToSongResponse(song);
                    } else if (content instanceof Playlist playlist) {
                        return convertToPlaylistResponse(playlist);
                    } else if (content instanceof Album album) {
                        return convertToAlbumResponse(album);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        // Tạo phản hồi
        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedContent);
        response.put("currentPage", page + 1);
        response.put("totalPages", totalPages);
        response.put("totalElements", totalElements);

        return response;
    }

    @Override
    public Map<String, Object> getContents(String title, Long genreId, String type, Integer limit, Integer offset) {
        String titleFilter = (title != null && !title.isEmpty()) ? title.trim().toLowerCase() : null;

        List<Song> songs = songRepository.getAllSongsByTitleOrGenreSongs(
                titleFilter,
                genreId,
                limit,
                offset
        );
        List<SongResponse> songResponseList = songs.stream()
                .map(this::convertToSearchSongResponse)
                .collect(Collectors.toList());

        List<Playlist> playlists = playListRepository.getAllPlaylistsByTitle(
                titleFilter,
                limit,
                offset
        );
        List<PlaylistResponse> playlistResponseList = playlists.stream()
                .map(this::convertToSearchPlaylistResponse)
                .collect(Collectors.toList());

        List<Album> albums = albumRepository.getAllAlbumsByTitle(
                titleFilter,
                limit,
                offset
        );
        List<AlbumResponse> albumResponseList = albums.stream()
                .map(this::convertToSearchAlbumResponse)
                .collect(Collectors.toList());

        List<Artist> artists = artistRepository.getAllArtistsByTitle(
                titleFilter,
                limit,
                offset
        );
        List<SearchArtistPresentation> artistPresentations = artists.stream()
                .map(this::convertToSearchArtistPresentation)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        switch (type.toLowerCase()) {
            case "all":
                List<Object> combinedContent = new ArrayList<>();
                combinedContent.addAll(songResponseList);
                combinedContent.addAll(playlistResponseList);
                combinedContent.addAll(albumResponseList);
                combinedContent.addAll(artistPresentations);
                response.put("content", combinedContent);
                break;
            case "songs":
                response.put("content", songResponseList);
                break;
            case "playlists":
                response.put("content", playlistResponseList);
                break;
            case "albums":
                response.put("content", albumResponseList);
                break;
            case "artists":
                response.put("content", artistPresentations);
                break;
            default:
                response.put("content", Collections.emptyList());
                response.put("error", "Invalid type: " + type);
                break;
        }

        return response;
    }

    // Helper Method
    private String getTitle(Object content) {
        if (content instanceof Song song) {
            return song.getTitle() != null ? song.getTitle() : "";
        } else if (content instanceof Playlist playlist) {
            return playlist.getPlaylistName() != null ? playlist.getPlaylistName() : "";
        } else if (content instanceof Album album) {
            return album.getAlbumName() != null ? album.getAlbumName() : "";
        }
        return "";
    }

    private Instant getReleaseDate(Object content) {
        if (content instanceof Song song) {
            return song.getReleaseDate();
        } else if (content instanceof Playlist playlist) {
            return playlist.getReleaseDate();
        } else if (content instanceof Album album) {
            return album.getReleaseDate();
        }
        return null;
    }

    private UserPresentation convertToUserPresentation(User user) {
        String formattedDate = user.getBirthDay() != null
                ? user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = user.getCreatedDate() != null
                ? formatter.format(user.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = user.getLastModifiedDate() != null
                ? formatter.format(user.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
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
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .userType(user.getUserType())
                .build();
    }

    private AdminPresentation convertToAdminPresentation(User admin) {
        String formattedDate = admin.getBirthDay() != null
                ? admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = admin.getCreatedDate() != null
                ? formatter.format(admin.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = admin.getLastModifiedDate() != null
                ? formatter.format(admin.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
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
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .userType(admin.getUserType())
                .build();
    }

    private ArtistPresentation convertToArtistPresentation(Artist artist) {
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
                .countListen(artist.getCountListen() != null ? artist.getCountListen() : 0)
                .userType(artist.getUserType())
                .build();
    }

    private SearchArtistPresentation convertToSearchArtistPresentation(Artist artist) {
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

        return SearchArtistPresentation.builder()
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
                .countListen(artist.getCountListen() != null ? artist.getCountListen() : 0)
                .userType(artist.getUserType())
                .artistSongIds(
                        artist.getArtistSongs().stream()
                                .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                .collect(Collectors.toList())
                )
                .artistPlaylistIds(
                        artist.getArtistPlaylists().stream()
                                .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                .collect(Collectors.toList())
                )
                .artistAlbumIds(
                        artist.getArtistAlbums().stream()
                                .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                .collect(Collectors.toList())
                )
                .build();
    }

    private SongResponse convertToSongResponse(Song song) {
        Long id = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

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

        if (user.getUserType() == UserType.ARTIST) {
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
                    .downloadPermission(song.getDownloadPermission() != null ? song.getDownloadPermission() : false)
                    .description(song.getDescription() != null ? song.getDescription() : "")
                    .mp3Url(song.getMp3Url() != null ? song.getMp3Url() : "")
                    .songStatus(song.getSongStatus() != null ? song.getSongStatus().name() : null)
                    .genreNameList(genreNames)
                    .artistNameList(additionalArtistNameList)
                    .numberOfListeners(userSongCountRepository.countDistinctUsersBySongId(song.getId()))
                    .countListen(userSongCountRepository.getTotalCountListenBySongId(song.getId()))
                    .numberOfDownload(userSongDownloadRepository.countDistinctUsersBySongId(song.getId()))
                    .numberOfUserLike(userSongLikeRepository.countDistinctUsersBySongId(song.getId()))
                    .build();
        } else {
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
    }

    private SongResponse convertToSearchSongResponse(Song song) {
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

    private PlaylistResponse convertToPlaylistResponse(Playlist playlist) {
        Long id = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

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

        if (user.getUserType() == UserType.ARTIST) {
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
                    .songNameList(songNameList)
                    .artistNameList(additionalArtistNameList)
                    .imageUrl(playlist.getImageUrl())
                    .status(playlist.getPlaylistAndAlbumStatus().toString())
                    .build();
        } else {
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
                    .songNameList(songNameList)
                    .artistNameList(artistNameList)
                    .imageUrl(playlist.getImageUrl())
                    .status(playlist.getPlaylistAndAlbumStatus().toString())
                    .build();
        }
    }

    private PlaylistResponse convertToSearchPlaylistResponse(Playlist playlist) {
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
                .songNameList(songNameList)
                .artistNameList(artistNameList)
                .imageUrl(playlist.getImageUrl())
                .status(playlist.getPlaylistAndAlbumStatus().toString())
                .build();
    }

    private AlbumResponse convertToAlbumResponse(Album album) {
        Long id = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

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

        if (user.getUserType() == UserType.ARTIST) {
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
                    .artistNameList(additionalArtistList)
                    .imageUrl(album.getImageUrl())
                    .status(album.getPlaylistAndAlbumStatus().toString())
                    .build();
        } else {
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
                    .songNameList(songNameList)
                    .artistNameList(artistNameList)
                    .imageUrl(album.getImageUrl())
                    .status(album.getPlaylistAndAlbumStatus().toString())
                    .build();
        }
    }

    private AlbumResponse convertToSearchAlbumResponse(Album album) {
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
                .songNameList(songNameList)
                .artistNameList(artistNameList)
                .imageUrl(album.getImageUrl())
                .status(album.getPlaylistAndAlbumStatus().toString())
                .build();
    }

    private GenreResponse convertToGenreResponse(Genre genre) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = genre.getCreatedDate() != null
                ? formatter.format(genre.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = genre.getLastModifiedDate() != null
                ? formatter.format(genre.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .briefDescription(genre.getBriefDescription())
                .fullDescription(genre.getFullDescription())
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .build();
    }

    // Pageable for Song Methods
    private Pageable createSongPageable(int page, int size, String orderBy, String order) {
        Sort sort = switch (orderBy) {
            case "title" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("title").ascending() : Sort.by("title").descending();
            case "releaseDate" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("releaseDate").ascending() : Sort.by("releaseDate").descending();
            default -> Sort.unsorted(); // numberOfListeners, countListen cho songs
        };

        return PageRequest.of(page - 1, size, sort);
    }

    private Pageable createPlaylistPageable(int page, int size, String orderBy, String order) {
        Sort sort = switch (orderBy) {
            case "title" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("playlistName").ascending() : Sort.by("playlistName").descending();
            case "releaseDate" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("releaseDate").ascending() : Sort.by("releaseDate").descending();
            default -> Sort.unsorted();
        };

        return PageRequest.of(page - 1, size, sort);
    }

    private Pageable createAlbumPageable(int page, int size, String orderBy, String order) {
        Sort sort = switch (orderBy) {
            case "title" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("albumName").ascending() : Sort.by("albumName").descending();
            case "releaseDate" ->
                    order.equalsIgnoreCase("asc") ? Sort.by("releaseDate").ascending() : Sort.by("releaseDate").descending();
            default -> Sort.unsorted();
        };

        return PageRequest.of(page - 1, size, sort);
    }

    private Map<String, Object> buildSongsPaginationResult(Page<Song> songsPage) {
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

    private Map<String, Object> buildPlaylistPaginationResult(Page<Playlist> playlistPage) {
        List<PlaylistResponse> playlistResponses = playlistPage.getContent().stream()
                .map(this::convertToPlaylistResponse)
                .toList();

        int currentPage = playlistPage.getNumber() + 1;
        int totalPages = playlistPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("playlists", playlistResponses);
        result.put("currentPage", currentPage);
        result.put("totalPages", totalPages);
        result.put("totalElements", playlistPage.getTotalElements());
        return result;
    }

    private Map<String, Object> buildAlbumsPaginationResult(Page<Album> albumPage) {
        List<AlbumResponse> albumResponses = albumPage.getContent().stream()
                .map(this::convertToAlbumResponse)
                .toList();

        int currentPage = albumPage.getNumber() + 1;
        int totalPages = albumPage.getTotalPages();
        if (currentPage > totalPages && totalPages > 0) {
            throw new BusinessException(ApiResponseCode.PAGE_OUT_OF_BOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("albums", albumResponses);
        result.put("currentPage", currentPage);
        result.put("totalPages", totalPages);
        result.put("totalElements", albumPage.getTotalElements());
        return result;
    }
}
