package com.spring.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.constants.*;
import com.spring.dto.response.*;
import org.springframework.context.event.EventListener;
import com.spring.dto.SongUploadedEvent;
import com.spring.dto.request.music.AdminAddSongRequest;
import com.spring.dto.request.music.EditSongRequest;
import com.spring.dto.request.music.SongUploadRequest;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.CloudinaryService;
import com.spring.service.FastApiService;
import com.spring.service.SongService;
import com.spring.utils.JavaFileToMultipartFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ArtistSongRepository artistSongRepository;
    private final GenreRepository genreRepository;
    private final GenreSongRepository genreSongRepository;
    private final UserSongDownloadRepository userSongDownloadRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final ArtistUserFollowRepository artistUserFollowRepository;
    private final FastApiService fastApiService;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private static final Logger log = LoggerFactory.getLogger(SongServiceImpl.class);

    // Initialize HttpClient for concurrent downloads
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    @Value("${file.upload-dir}")
    private String uploadDirPath;

    @PostConstruct
    public void init() throws IOException {
        Path uploadDir = Paths.get(uploadDirPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    // TODO: Helper method
    public SongUploadResponse enrichUploadData(SongUploadRequest songUploadRequest) {
        // Upload thông tin
        String title = songUploadRequest.getTitle();
        String lyrics = songUploadRequest.getLyrics();
        String description = songUploadRequest.getDescription();
        Boolean downloadPermission = songUploadRequest.getDownloadPermission();

        // Upload file lên Cloudinary
        MultipartFile mp3File = songUploadRequest.getFile();
        SongUploadResponse uploaded = cloudinaryService.uploadAudioToCloudinary(songUploadRequest.getFile());
        String audioUrl = uploaded.getMp3Url();
        String formattedDuration = uploaded.getDuration();

        MultipartFile image = songUploadRequest.getImage();
        String imageUrl = cloudinaryService.uploadImageToCloudinary(image);

        return SongUploadResponse.builder()
                .mp3File(mp3File)
                .mp3Url(audioUrl)
                .image(image)
                .imageUrl(imageUrl)
                .duration(formattedDuration)
                .title(title)
                .lyrics(lyrics)
                .downloadPermission(downloadPermission != null ? downloadPermission : false)
                .description(description)
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

    private FastApiResponse extractResponseFromJson(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseBody, FastApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi phân tích JSON: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<Path> downloadAudioFromCloudinary(String cloudinaryUrl, String destinationFolder, String name) {
        try {
            // Generate unique file name to avoid conflicts
            String uniqueFileName = name + ".mp3";
            Path destinationPath = Paths.get(destinationFolder, uniqueFileName);

            // Ensure destination directory exists (thread-safe)
            Files.createDirectories(Paths.get(destinationFolder));

            // Create HTTP request for downloading the audio
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(cloudinaryUrl))
                    .GET()
                    .build();

            // Perform async HTTP request and save to file
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            log.error("❌ Failed to download audio from {}: HTTP {}", cloudinaryUrl, response.statusCode());
                            throw new RuntimeException("HTTP error: " + response.statusCode());
                        }
                        try (InputStream in = response.body()) {
                            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                            log.info("✅ Audio downloaded to: {}", destinationPath.toAbsolutePath());
                            return destinationPath;
                        } catch (IOException e) {
                            log.error("❌ Failed to save audio to {}: {}", destinationPath, e.getMessage());
                            throw new RuntimeException("Failed to save audio", e);
                        }
                    })
                    .exceptionally(throwable -> {
                        log.error("❌ Error downloading audio from {}: {}", cloudinaryUrl, throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("❌ Failed to initiate download from {}: {}", cloudinaryUrl, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    // TODO: Main function
    @Override
    public ApiResponse createDraftSong(SongUploadRequest songUploadRequest) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();
        Long artistId = jwtHelper.getIdUserRequesting();

        // Lấy dữ liệu bổ sung từ request
        SongUploadResponse enriched = enrichUploadData(songUploadRequest);
        String title = enriched.getTitle();

        String lyrics = enriched.getLyrics();
        if (lyrics != null && !lyrics.isBlank() && lyrics.length() <= 100) {
            return ApiResponse.ok("Lyrics of the song is too short, Lyrics must be at least above 100 words!");
        }

        // Lưu song
        Song song = Song.builder()
                .title(title)
                .releaseDate(now)
                .lyrics(lyrics)
                .duration(enriched.getDuration())
                .imageUrl(enriched.getImageUrl())
                .downloadPermission(enriched.getDownloadPermission())
                .description(enriched.getDescription())
                .mp3Url(enriched.getMp3Url())
                .countListener(0L)
                .songStatus(SongStatus.DRAFT)
                .createdDate(now)
                .lastModifiedDate(now)
                .build();
        songRepository.save(song);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        artistSongRepository.save(new ArtistSong(new ArtistSongId(song, artist)));

        if (songUploadRequest.getGenreIds() != null && !songUploadRequest.getGenreIds().isEmpty()) {
            for (Long genreId : songUploadRequest.getGenreIds()) {
                Genre genre = genreRepository.findById(genreId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                genreSongRepository.save(new GenreSong(new GenreSongId(song, genre)));
            }
        }

        if (songUploadRequest.getArtistIds() != null && !songUploadRequest.getArtistIds().isEmpty()) {
            for (Long additionalArtistId : songUploadRequest.getArtistIds()) {
                Artist additionalArtist = artistRepository.findById(additionalArtistId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                if (additionalArtist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                    throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                }

                artistSongRepository.save(new ArtistSong(new ArtistSongId(song, additionalArtist)));
            }
        }

        return ApiResponse.ok("Creat draft successful!");
    }

    @Override
    public ApiResponse uploadSong(Long songId) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (song.getSongStatus() != SongStatus.DRAFT) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        song.setSongStatus(SongStatus.PROCESSING);
        song.setLastModifiedDate(now);
        songRepository.save(song);

        applicationEventPublisher.publishEvent(new SongUploadedEvent(this, song.getId()));

        return ApiResponse.ok("Upload song successful!");
    }

    @EventListener
    @Async
    public CompletableFuture<ApiResponse> processSong(SongUploadedEvent songUploadedEvent) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        String message;
        Long songId = songUploadedEvent.getSongId();
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (song.getSongStatus() != SongStatus.PROCESSING) {
            message = "Song with ID [" + songId + "] not in PROCESSING state.";
            log.warn("❗ {}", message);
            return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
        }

        boolean isFirstSong = songRepository.countAllSongs() == 0;
        File file = null;

        try {
            // Download audio asynchronously
            CompletableFuture<Path> downloadFuture = downloadAudioFromCloudinary(song.getMp3Url(), uploadDirPath, song.getTitle());
            Path filePath = downloadFuture.join(); // Wait for download to complete
            if (filePath == null) {
                message = "Failed to download audio for Song [" + song.getId() + "]";
                log.error("❌ {}", message);
                return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
            }

            file = filePath.toFile();
            if (!file.exists()) {
                message = "Local file not found: " + filePath;
                log.error("❗ {}", message);
                return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
            }

            MultipartFile mp3File = new JavaFileToMultipartFile(file);
            FastApiResponse fastApiResult;
            String requestId;

            if (!isFirstSong) {
                // 1. Check lyrics similarity
                if (song.getLyrics() != null && !song.getLyrics().isBlank()) {
                    fastApiResult = extractResponseFromJson(fastApiService.checkLyricsSimilarity(song.getLyrics()).getBody());
                    if (Boolean.TRUE.equals(fastApiResult.getMatch())) {
                        message = "Song [" + song.getId() + "]: Lyrics match another song.";
                        log.warn("❌ {}", message);
                        song.setSongStatus(SongStatus.DECLINED);
                        songRepository.save(song);
                        return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
                    }
                }

                // 2. Check audio similarity
                fastApiResult = extractResponseFromJson(fastApiService.checkAudioSimilarity(mp3File).getBody());
                if (Boolean.TRUE.equals(fastApiResult.getMatch())) {
                    message = "Song [" + song.getId() + "]: Audio matches another song.";
                    log.warn("❌ {}", message);
                    song.setSongStatus(SongStatus.DECLINED);
                    songRepository.save(song);
                    return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
                }

                requestId = fastApiResult.getRequestId();

                // 3. Lyrics transcription or cross-check
                String transcribedLyrics = fastApiResult.getLyrics();
                if (song.getLyrics() == null || song.getLyrics().isBlank()) {
                    song.setLyrics(transcribedLyrics);
                } else {
                    FastApiResponse checkResult = extractResponseFromJson(
                            fastApiService.checkSimilarityBetweenLyricsAndAudio(song.getLyrics(), transcribedLyrics).getBody()
                    );
                    if (Boolean.TRUE.equals(checkResult.getIsNotMatch())) {
                        message = "Song [" + song.getId() + "]: Lyrics and audio do not match.";
                        log.warn("⚠️ {}", message);
                        song.setSongStatus(SongStatus.DECLINED);
                        songRepository.save(song);
                        return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
                    }
                }
            } else {
                // Transcribe only
                fastApiResult = extractResponseFromJson(fastApiService.transcribeLyricsFromAudio(mp3File).getBody());
                String transcribedLyrics = fastApiResult.getLyrics();
                requestId = fastApiResult.getRequestId();

                if (song.getLyrics() == null || song.getLyrics().isBlank()) {
                    song.setLyrics(transcribedLyrics);
                } else {
                    FastApiResponse checkResult = extractResponseFromJson(
                            fastApiService.checkSimilarityBetweenLyricsAndAudio(song.getLyrics(), transcribedLyrics).getBody()
                    );
                    if (Boolean.TRUE.equals(checkResult.getIsNotMatch())) {
                        message = "Song [" + song.getId() + "]: Lyrics and audio do not match.";
                        log.warn("⚠️ {}", message);
                        song.setSongStatus(SongStatus.DECLINED);
                        songRepository.save(song);
                        return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing failed", message));
                    }
                }
            }

            // 4. Predict genres & save
            fastApiService.handleGenres(song, null, song.getLyrics(), mp3File, requestId);
            song.setSongStatus(SongStatus.PENDING);
            song.setLastModifiedDate(now);
            songRepository.save(song);
            message = "Song [" + song.getId() + "] processed successfully.";
            log.info("✅ {}", message);
        } catch (Exception ex) {
            message = "Song [" + song.getId() + "] failed with error: " + ex.getMessage();
            log.error("❗ {}", message, ex);
        } finally {
            // Ensure file deletion
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("🧹 Deleted temporary file: {}", file.getAbsolutePath());
                } else {
                    log.warn("⚠️ Failed to delete temporary file: {}", file.getAbsolutePath());
                }
            }
        }

        return CompletableFuture.completedFuture(ApiResponse.ok("🎶 Processing completed", message));
    }

    @Override
    public ApiResponse publishSong(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Song song = songRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (song.getSongStatus() != SongStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        } else {
            song.setSongStatus(SongStatus.ACCEPTED);
            song.setCountListener(0L);
            song.setLastModifiedDate(now);
            songRepository.save(song);
            return ApiResponse.ok("Song accepted!");
        }
    }

    @Override
    public ApiResponse declineSong(Long id) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Song song = songRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (song.getSongStatus() != SongStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        } else {
            song.setSongStatus(SongStatus.DECLINED);
            song.setLastModifiedDate(now);
            songRepository.save(song);
            return ApiResponse.ok("Song declined!");
        }
    }

    @Override
    public ApiResponse updateSong(Long songId, EditSongRequest request) {
        Long artistId = jwtHelper.getIdUserRequesting();
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        if (song.getSongStatus() == SongStatus.PROCESSING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            song.setTitle(request.getTitle());
        }

        if (request.getLyrics() != null && !request.getLyrics().isBlank()) {
            song.setLyrics(request.getLyrics());
        }

        if (request.getDownloadPermission() != null) {
            song.setDownloadPermission(request.getDownloadPermission());
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            song.setDescription(request.getDescription());
        }

        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            List<Long> newGenreIds = request.getGenreIds();

            List<Long> currentGenreIds = song.getGenreSongs().stream()
                    .map(gs -> gs.getGenreSongId().getGenre().getId())
                    .toList();

            for (Long genreId : newGenreIds) {
                if (!currentGenreIds.contains(genreId)) {
                    Genre genre = genreRepository.findById(genreId)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
                    song.getGenreSongs().add(new GenreSong(new GenreSongId(song, genre)));
                }
            }

            song.getGenreSongs().removeIf(gs -> !newGenreIds.contains(gs.getGenreSongId().getGenre().getId()));
        } else {
            song.getGenreSongs().clear();
        }

        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            List<Long> newAdditionalArtistIds = request.getArtistIds().stream()
                    .filter(id -> !id.equals(artistId))
                    .toList();

            List<Long> currentAdditionalArtistIds = song.getArtistSongs().stream()
                    .map(ap -> ap.getArtistSongId().getArtist().getId())
                    .filter(id -> !id.equals(artistId))
                    .toList();

            for (Long additionalArtistId : newAdditionalArtistIds) {
                if (!currentAdditionalArtistIds.contains(additionalArtistId)) {
                    Artist additionalArtist = artistRepository.findById(additionalArtistId)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

                    if (additionalArtist.getStatus() == CommonStatus.INACTIVE.getStatus()) {
                        throw new BusinessException(ApiResponseCode.INVALID_STATUS);
                    }

                    artistSongRepository.save(new ArtistSong(new ArtistSongId(song, additionalArtist)));
                }
            }

            song.getArtistSongs().removeIf(ap -> {
                Long id = ap.getArtistSongId().getArtist().getId();
                return !id.equals(artistId) && !newAdditionalArtistIds.contains(id);
            });
        } else {
            song.getArtistSongs().removeIf(ap -> {
                Long id = ap.getArtistSongId().getArtist().getId();
                return !id.equals(artistId);
            });
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());
            song.setImageUrl(imageUrl);
        }

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            SongUploadResponse uploaded = cloudinaryService.uploadAudioToCloudinary(request.getFile());
            String audioUrl = uploaded.getMp3Url();
            String formattedDuration = uploaded.getDuration();

            song.setMp3Url(audioUrl);
            song.setDuration(formattedDuration);
        }

        song.setLastModifiedDate(now);
        song.setSongStatus(song.getSongStatus());
        Song updated = songRepository.save(song);
        songRepository.save(updated);

        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse deleteSong(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        // Xoá bài hát
        songRepository.delete(song);

        return ApiResponse.ok("Xoá bài hát thành công!");
    }

    @Override
    public List<SongResponse> getAllSongs() {
        List<Song> songs = songRepository.findAll();

        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponse> getAllSongsByArtistId(Long artistId) {
        List<Song> songs = songRepository.findByArtistId(artistId);
        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponse> getAllAcceptedSongsByArtistId(Long artistId) {
        List<Song> songs = songRepository.findByArtistId(artistId);
        return songs.stream()
                .filter(p -> p.getSongStatus().equals(SongStatus.ACCEPTED))
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponse> getAllAcceptedSongsByPlaylistId(Long playlistId) {
        List<Song> songs = songRepository.findByPlaylistId(playlistId);
        return songs.stream()
                .filter(p -> p.getSongStatus().equals(SongStatus.ACCEPTED))
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());    }

    @Override
    public List<SongResponse> getAllAcceptedSongsByAlbumId(Long albumId) {
        List<Song> songs = songRepository.findByAlbumId(albumId);
        return songs.stream()
                .filter(p -> p.getSongStatus().equals(SongStatus.ACCEPTED))
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SongResponse getSongById(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        return convertToSongResponse(song);
    }

    @Override
    public List<SongResponse> getSongsByGenre(Long genreId) {
        List<GenreSong> genreSongs = genreSongRepository.findAllByGenreId(genreId);
        List<Song> songs = genreSongs.stream()
                .map(gs -> gs.getGenreSongId().getSong())
                .toList();
        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getNumberOfListener(Long songId) {
        return userSongCountRepository.countDistinctUsersBySongId(songId);
    }

    @Override
    public Long getCountListen(Long songId) {
        return userSongCountRepository.getTotalCountListenBySongId(songId);
    }

    @Override
    public List<SongResponse> getAcceptedSongsByArtistId() {
        Long artistId = jwtHelper.getIdUserRequesting();
        List<Song> songs = songRepository.findByArtistIdAndStatus(artistId, SongStatus.ACCEPTED);
        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponse> getAllAcceptedSongs() {
        List<Song> songs = songRepository.findAllBySongStatus(SongStatus.ACCEPTED);
        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getNumberOfDownload(Long songId) {
        return userSongDownloadRepository.countDistinctUsersBySongId(songId);
    }

    @Override
    public List<SongResponse> getTrendingSongs() {
        List<Object[]> topSongs = userSongCountRepository.findTopSongsByListenCount(10, 0);

        List<Song> songs = topSongs.stream()
                .map(obj -> {
                    Long songId = ((Number) obj[0]).longValue();
                    return songRepository.findById(songId).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();

        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponse> getTop15BestSongEachGenre(Long genreId) {
        List<GenreSong> genreSongs = genreSongRepository.findAllByGenreId(genreId);

        List<Song> songs = genreSongs.stream()
                .map(gs -> gs.getGenreSongId().getSong())
                .distinct()
                .sorted((s1, s2) -> {
                    Long count1 = userSongDownloadRepository.countDistinctUsersBySongId(s1.getId());
                    Long count2 = userSongDownloadRepository.countDistinctUsersBySongId(s2.getId());
                    return count2.compareTo(count1); // Sắp giảm dần
                })
                .limit(15)
                .toList();

        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Long getNumberOfUserLike(Long songId) {
        return userSongLikeRepository.countDistinctUsersBySongId(songId);
    }

    @Override
    public ApiResponse addSongRequest(AdminAddSongRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        // Upload thông tin
        String title = request.getTitle();
        String lyrics = request.getLyrics();
        String description = request.getDescription();
        Boolean downloadPermission = request.getDownloadPermission();

        // Upload file lên Cloudinary
        MultipartFile mp3File = request.getFile();
        SongUploadResponse uploaded = cloudinaryService.uploadAudioToCloudinary(mp3File);
        String audioUrl = uploaded.getMp3Url();
        String formattedDuration = uploaded.getDuration();

        MultipartFile image = request.getImage();
        String imageUrl = cloudinaryService.uploadImageToCloudinary(image);

        // Tạo bài hát mới
        Song song = Song.builder()
                .title(title)
                .lyrics(lyrics)
                .description(description)
                .mp3Url(audioUrl)
                .imageUrl(imageUrl)
                .duration(formattedDuration)
                .releaseDate(now)
                .downloadPermission(downloadPermission != null ? downloadPermission : false)
                .songStatus(SongStatus.ACCEPTED)
                .countListener(0L)
                .createdDate(now)
                .lastModifiedDate(now)
                .build();
        song = songRepository.save(song);

        List<Long> artistIds = request.getArtistIds();
        for (Long artistId : artistIds) {
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
            artistSongRepository.save(new ArtistSong(new ArtistSongId(song, artist)));
        }

        List<Genre> genres = request.getGenreIds().stream()
                .map(genreId -> genreRepository.findById(genreId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND)))
                .toList();
        for (Genre genre : genres) {
            genreSongRepository.save(new GenreSong(new GenreSongId(song, genre)));
        }

        return ApiResponse.ok("Thêm bài hát thành công!");
    }

    @Override
    public Long totalSongsByArtist() {
        Long artistId = jwtHelper.getIdUserRequesting();
        return songRepository.findByArtistId(artistId).stream().count();
    }

    @Override
    public Long totalNumberOfListeners() {
        Long artistId = jwtHelper.getIdUserRequesting();
        List<Song> songs = songRepository.findByArtistId(artistId).stream()
                .filter(song -> !song.getSongStatus().equals(SongStatus.ACCEPTED))
                .toList();
        if (songs.isEmpty()) {
            return 0L;
        }
        List<Long> songIds = songs.stream().map(Song::getId).collect(Collectors.toList());
        return userSongCountRepository.countDistinctListenersBySongIds(songIds);
    }

    @Override
    public Long totalNumberOfDownloads() {
        Long artistId = jwtHelper.getIdUserRequesting();
        List<Song> songs = songRepository.findByArtistId(artistId).stream()
                .filter(song -> !song.getSongStatus().equals(SongStatus.ACCEPTED))
                .toList();
        if (songs.isEmpty()) {
            return 0L;
        }
        List<Long> songIds = songs.stream().map(Song::getId).collect(Collectors.toList());
        return userSongDownloadRepository.countDistinctListenersBySongIds(songIds);
    }

    @Override
    public Long totalNumberOfLikes() {
        Long artistId = jwtHelper.getIdUserRequesting();
        List<Song> songs = songRepository.findByArtistId(artistId).stream()
                .filter(song -> !song.getSongStatus().equals(SongStatus.ACCEPTED))
                .toList();
        if (songs.isEmpty()) {
            return 0L;
        }
        List<Long> songIds = songs.stream().map(Song::getId).collect(Collectors.toList());
        return userSongLikeRepository.countDistinctListenersBySongIds(songIds);
    }

    @Override
    public Long totalNumberOfUserFollowers() {
        Long artistId = jwtHelper.getIdUserRequesting();
        return artistUserFollowRepository.countDistinctUsersByArtistId(artistId);
    }
}