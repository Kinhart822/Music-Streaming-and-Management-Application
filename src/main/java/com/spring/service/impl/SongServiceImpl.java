package com.spring.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.constants.ApiResponseCode;
import com.spring.constants.ManageProcess;
import com.spring.constants.SongStatus;
import org.springframework.context.event.EventListener;
import com.spring.dto.SongUploadedEvent;
import com.spring.dto.request.music.admin.AdminAddSongRequest;
import com.spring.dto.request.music.artist.EditSongRequest;
import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.FastApiResponse;
import com.spring.dto.response.SongResponse;
import com.spring.dto.response.SongUploadResponse;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final ArtistSongRepository artistSongRepository;
    private final GenreRepository genreRepository;
    private final GenreSongRepository genreSongRepository;
    private final UserSongDownloadRepository userSongDownloadRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final FastApiService fastApiService;
    private final JwtHelper jwtHelper;
    private final CloudinaryService cloudinaryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private static final Logger log = LoggerFactory.getLogger(SongServiceImpl.class);

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
        String title = songUploadRequest.getTitle();
        String lyrics = songUploadRequest.getLyrics();
        Boolean downloadPermission = songUploadRequest.getDownloadPermission();
        String description = songUploadRequest.getDescription();
        List<Long> genreId = songUploadRequest.getGenreId();
        MultipartFile image = songUploadRequest.getImage();
        String imageUrl = cloudinaryService.uploadImageToCloudinary(image);

        List<String> genreNames = genreId.stream()
                .map(id -> genreRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND)))
                .map(Genre::getGenresName)
                .toList();

        return SongUploadResponse.builder()
                .imageUrl(imageUrl)
                .title(title)
                .lyrics(lyrics)
                .downloadPermission(downloadPermission != null ? downloadPermission : false)
                .description(description)
                .genreNameList(genreNames)
                .build();
    }

    private SongResponse convertToSongResponse(Song song) {
        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .releaseDate(song.getReleaseDate() != null ? song.getReleaseDate().toString() : null)
                .lyrics(song.getLyrics())
                .duration(song.getDuration())
                .imageUrl(song.getImageUrl())
                .artSmallUrl(song.getArtSmallUrl())
                .artMediumUrl(song.getArtMediumUrl())
                .artBigUrl(song.getArtBigUrl())
                .downloadPermission(song.getDownloadPermission())
                .description(song.getDescription())
                .mp3Url(song.getMp3Url())
                .trackUrl(song.getTrackUrl())
                .songStatus(song.getSongStatus() != null ? song.getSongStatus().name() : null)
                .build();
    }

    private FastApiResponse extractResponseFromJson(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseBody, FastApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi phân tích JSON: " + e.getMessage(), e);
        }
    }

    public Path downloadAudioFromCloudinary(String cloudinaryUrl, String destinationFolder, String name) {
        try {
            URL url = new URL(cloudinaryUrl);
            // Tạo tên file mới
            String newFileName = name + ".mp3";
            Path destinationPath = Paths.get(destinationFolder, newFileName);

            Files.createDirectories(Paths.get(destinationFolder));

            try (InputStream in = url.openStream()) {
                Files.copy(in, destinationPath);
                log.info("✅ Audio downloaded to: {}", destinationPath.toAbsolutePath());
            }

            return destinationPath;
        } catch (IOException e) {
            log.error("❌ Failed to download audio: {}", e.getMessage());
            return null;
        }
    }

    // TODO: Main function
    @Override
    public ApiResponse uploadSong(SongUploadRequest songUploadRequest) {
        Long artistId = jwtHelper.getIdUserRequesting();

        // Upload file lên Cloudinary
        SongUploadResponse uploaded = cloudinaryService.uploadAudioToCloudinary(songUploadRequest.getFile());
        String audioUrl = uploaded.getMp3Url();
        String formattedDuration = uploaded.getDuration();

        // Lấy dữ liệu bổ sung từ request
        SongUploadResponse enriched = enrichUploadData(songUploadRequest);
        String title = enriched.getTitle();

        String lyrics = enriched.getLyrics();
        if (lyrics != null && !lyrics.isBlank() && lyrics.length() <= 100) {
            return ApiResponse.ok("Lyrics of the song is too short, Lyrics must be at least above 100 words!");
        }

        String description = enriched.getDescription();
        Boolean downloadPermission = enriched.getDownloadPermission();
        String imageUrl = enriched.getImageUrl();

        // Lưu song
        Song song = Song.builder()
                .title(title)
                .releaseDate(Date.from(Instant.now()))
                .lyrics(lyrics)
                .duration(formattedDuration)
                .imageUrl(imageUrl)
                .downloadPermission(downloadPermission)
                .description(description)
                .mp3Url(audioUrl)
                .songStatus(SongStatus.PROCESSING)
                .build();
        songRepository.save(song);

        // Gán artist cho bài hát
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        artistSongRepository.save(new ArtistSong(new ArtistSongId(artist, song)));

        applicationEventPublisher.publishEvent(new SongUploadedEvent(this, song.getId()));

        return ApiResponse.ok("Upload successful! Your song is pending for processing."
                + (audioUrl != null ? " Audio URL: " + audioUrl : "")
                + (imageUrl != null ? " | Image URL: " + imageUrl : ""));
    }

    @EventListener
    @Async
    public CompletableFuture<ApiResponse> processSong(SongUploadedEvent songUploadedEvent) {
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
        Path filePath;
        File file = null;

        try {
            filePath = downloadAudioFromCloudinary(song.getMp3Url(), uploadDirPath, song.getTitle());
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
    public ApiResponse manageUploadSong(Long id, String manageProcess) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (song.getSongStatus() != SongStatus.PENDING) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        if (manageProcess.equalsIgnoreCase(ManageProcess.ACCEPTED.name())) {
            song.setSongStatus(SongStatus.ACCEPTED);
            song.setCountListener(0L);
            songRepository.save(song);
            return ApiResponse.ok("Song accepted successfully!");
        } else if (manageProcess.equalsIgnoreCase(ManageProcess.DECLINED.name())) {
            song.setSongStatus(SongStatus.DECLINED);
            songRepository.save(song);
            return ApiResponse.ok("Song declined! Please check your song before uploading again.");
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }
    }

    @Override
    public ApiResponse updateSong(Long id, EditSongRequest request) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

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

        if (request.getGenreId() != null && !request.getGenreId().isEmpty()) {
            List<Genre> genres = request.getGenreId().stream()
                    .map(index -> genreRepository.findById(index)
                            .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND)))
                    .toList();

            List<GenreSong> genreSongs = genres.stream()
                    .map(genre -> new GenreSong(new GenreSongId(song, genre)))
                    .toList();

            song.setGenreSongs(genreSongs);
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

        songRepository.save(song);
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
    public ApiResponse getNumberOfListener(Long songId) {
        Long totalCountListener = userSongCountRepository.getTotalCountListenBySongId(songId);
        return ApiResponse.ok("Có tầm" + totalCountListener + "đã nghe bài hát");
    }

    @Override
    public ApiResponse getCountListen(Long songId) {
        Long totalCountListen = userSongCountRepository.getTotalCountListenBySongId(songId);
        return ApiResponse.ok("Bài hát được nghe " + totalCountListen + " lần");
    }

    @Override
    public List<SongResponse> getSongsByStatus(String status) {
        SongStatus songStatus = SongStatus.valueOf(status.toUpperCase());
        List<Song> songs = songRepository.findAllBySongStatus(songStatus);
        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponse> getSongsByStatusAndArtistId(String status) {
        Long artistId = jwtHelper.getIdUserRequesting();

        SongStatus songStatus;
        try {
            songStatus = SongStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        List<Song> songs = songRepository.findByArtistIdAndStatus(artistId, songStatus);

        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());

    }

    @Override
    public List<SongResponse> getSongsByArtistId() {
        Long artistId = jwtHelper.getIdUserRequesting();

        List<Song> songs = songRepository.findByArtistId(artistId);

        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse getNumberOfDownload(Long songId) {
        Long count = userSongDownloadRepository.countBySongId(songId);
        return ApiResponse.ok(String.valueOf(count));
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
                    Long count1 = userSongDownloadRepository.countBySongId(s1.getId());
                    Long count2 = userSongDownloadRepository.countBySongId(s2.getId());
                    return count2.compareTo(count1); // Sắp giảm dần
                })
                .limit(15)
                .toList();

        return songs.stream()
                .map(this::convertToSongResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse getNumberOfUserLike(Long songId) {
        Long count = userSongLikeRepository.countBySongId(songId);
        return ApiResponse.ok(count.toString());
    }

    @Override
    public ApiResponse addSongRequest(AdminAddSongRequest request) {
        String title = request.getTitle();
        String lyrics = request.getLyrics();
        String duration = request.getDuration();

        // Kiểm tra xem các nghệ sĩ tồn tại ko
        List<Artist> artists = request.getArtistId().stream()
                .map(artistId -> artistRepository.findById(artistId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND)))
                .toList();

        // Kiểm tra xem các thể loại tồn tại ko
        List<Genre> genres = request.getGenreId().stream()
                .map(genreId -> genreRepository.findById(genreId)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND)))
                .toList();

        // Tạo bài hát mới
        Song song = Song.builder()
                .title(title)
                .lyrics(lyrics)
                .duration(duration)
                .releaseDate(Date.from(Instant.now()))
                .downloadPermission(false)
                .songStatus(SongStatus.ACCEPTED)
                .countListener(0L)
                .build();
        song = songRepository.save(song);

        for (Artist artist : artists) {
            artistSongRepository.save(new ArtistSong(new ArtistSongId(artist, song)));
        }

        for (Genre genre : genres) {
            genreSongRepository.save(new GenreSong(new GenreSongId(song, genre)));
        }

        // Lưu lại bài hát với các thể loại đã liên kết
        songRepository.save(song);

        return ApiResponse.ok("Thêm bài hát thành công!");
    }

}
