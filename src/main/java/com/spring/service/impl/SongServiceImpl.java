package com.spring.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.constants.ApiResponseCode;
import com.spring.constants.SongStatus;
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
import com.spring.service.SongService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SongServiceImpl implements SongService {
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final ArtistSongRepository artistSongRepository;
    private final GenreRepository genreRepository;
    private final GenreSongRepository genreSongRepository;
    private final UserSongDownloadRepository userSongDownloadRepository;
    private final UserSongLikeRepository userSongLikeRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final JwtHelper jwtHelper;
    private final Cloudinary cloudinary;

    @Value("${file.upload-dir}")
    private String uploadDirPath;

    @PostConstruct
    public void init() throws IOException {
        Path uploadDir = Paths.get(uploadDirPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    public SongUploadResponse enrichUploadData(SongUploadRequest songUploadRequest) {
        String title = songUploadRequest.getTitle();
        String lyrics = songUploadRequest.getLyrics();
        Boolean downloadPermission = songUploadRequest.getDownloadPermission();
        String description = songUploadRequest.getDescription();
        List<Long> genreId = songUploadRequest.getGenreId();
        MultipartFile image = songUploadRequest.getImage();

        // Upload image if exists
        String imageUrl = null;
        try {
            if (image != null && !image.isEmpty()) {
                String imageOriginalFilename = image.getOriginalFilename();
                List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff");

                boolean isValidImage = allowedExtensions.stream()
                        .anyMatch(ext -> imageOriginalFilename.toLowerCase().endsWith(ext));

                if (!isValidImage) {
                    throw new IllegalArgumentException("Ảnh phải là một trong các định dạng: jpg, jpeg, png, gif, webp, bmp, tiff!");
                }

                imageUrl = uploadToCloudinary(image, "image", "covers");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage(), e);
        }

        List<String> genreNames = genreId.stream()
                .map(id -> genreRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id)))
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

    public SongUploadResponse uploadAudioFiles(SongUploadRequest songUploadRequest) {
        try {
            MultipartFile file = songUploadRequest.getFile();

            // Validate audio file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File mp3 không được để trống!");
            }

            String originalFilename = file.getOriginalFilename();
            if (!originalFilename.toLowerCase().endsWith(".mp3")) {
                throw new IllegalArgumentException("File phải có định dạng .mp3!");
            }

            // Upload audio to Cloudinary
            Map<?, ?> audioUploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "video",
                            "public_id", "songs/" + UUID.randomUUID() + "_" + originalFilename
                    )
            );

            String audioUrl = (String) audioUploadResult.get("secure_url");
            Double durationSeconds = (Double) audioUploadResult.get("duration");
            String formattedDuration = formatDuration(durationSeconds);

            return SongUploadResponse.builder()
                    .mp3File(file)
                    .mp3Url(audioUrl)
                    .duration(formattedDuration)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload bài hát: " + e.getMessage(), e);
        }
    }

    // Helper method to upload image to Cloudinary
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

    // Helper method
    private String formatDuration(Double seconds) {
        if (seconds == null) return "00:00";
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private FastApiResponse extractResponseFromJson(String responseBody) {
        try {
            if (responseBody == null || (!responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("["))) {
                throw new IOException("Response is not JSON: " + responseBody);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(responseBody);

            String requestId = root.has("request_id") ? root.get("request_id").asText() : null;
            Boolean match = root.has("match") ? root.get("match").asBoolean() : null;
            Double similarityScore = root.has("similarity_score") ? root.get("similarity_score").asDouble() : null;
            String genre = root.has("genre") ? root.get("genre").asText() : null;
            String message = root.has("message") ? root.get("message").asText() : null;

            return new FastApiResponse(requestId, match, similarityScore, genre, message);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi phân tích JSON: " + e.getMessage(), e);
        }
    }


    public ResponseEntity<String> predictGenre(
            @RequestParam("file") MultipartFile file,
            @RequestParam("request_id") String requestId
    ) {
        try {
            // URL FastAPI
            String fastApiUrl = "http://localhost:8000/predict-genre";

            // Prepare file resource
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @NotNull
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            // Prepare multipart form
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", fileAsResource);
            body.add("request_id", requestId);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Request
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            // Call FastAPI
            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, requestEntity, String.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    public ResponseEntity<String> predictGenreByLyrics(@RequestParam("lyrics") String lyrics) {
        try {
            String fastApiUrl = "http://localhost:8000/predict-genre";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("lyrics", lyrics);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, request, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    public ResponseEntity<String> checkSimilarity(@RequestParam("file") MultipartFile file) {
        try {
            // URL FastAPI
            String fastApiUrl = "http://localhost:8000/check-similarity";

            // Prepare file resource
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @NotNull
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            // Prepare multipart form
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", fileAsResource);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Request
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            // Call FastAPI
            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, requestEntity, String.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    public ResponseEntity<String> checkSimilarityByLyrics(@RequestParam("lyrics") String lyrics) {
        try {
            String fastApiUrl = "http://localhost:8000/check-similarity";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("lyrics", lyrics);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, request, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
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

    // Main function
    @Override
    public ApiResponse uploadSong(SongUploadRequest songUploadRequest) {
        Long artistId = jwtHelper.getIdUserRequesting();

        // Step 1: Enrich request data
        SongUploadResponse enriched = enrichUploadData(songUploadRequest);
        String title = enriched.getTitle();
        String lyrics = enriched.getLyrics();
        String description = enriched.getDescription();
        Boolean downloadPermission = enriched.getDownloadPermission();
        String imageUrl = enriched.getImageUrl();
        List<String> genreNameList = enriched.getGenreNameList();
        String requestId = null;

        try {
            // Step 2: Check lyric similarity
            if (lyrics != null && !lyrics.isBlank()) {
                ResponseEntity<String> lyricsCheckResponse = checkSimilarityByLyrics(lyrics);
                FastApiResponse similarityResult = extractResponseFromJson(lyricsCheckResponse.getBody());

                if (Boolean.TRUE.equals(similarityResult.getMatch())) {
                    return ApiResponse.ok("Bài hát có lời trùng khớp với bài khác! Xin mời upload lại!");
                }
            }

            // Step 3: Upload audio
            SongUploadResponse uploaded = uploadAudioFiles(songUploadRequest);
            MultipartFile mp3File = uploaded.getMp3File();
            String audioUrl = uploaded.getMp3Url();
            String formattedDuration = uploaded.getDuration();

            // Step 4: Check audio similarity
            if (mp3File != null) {
                ResponseEntity<String> mp3CheckResponse = checkSimilarity(mp3File);
                FastApiResponse audioSimilarityResult = extractResponseFromJson(mp3CheckResponse.getBody());

                if (Boolean.TRUE.equals(audioSimilarityResult.getMatch())) {
                    return ApiResponse.ok("Bài hát có file trùng khớp với bài khác! Xin mời upload lại!");
                }

                requestId = audioSimilarityResult.getRequestId();
            }

            // Step 5: Create and save Song
            Song song = Song.builder()
                    .title(title)
                    .releaseDate(Date.from(Instant.now()))
                    .lyrics(lyrics)
                    .duration(formattedDuration)
                    .imageUrl(imageUrl)
                    .downloadPermission(downloadPermission)
                    .description(description)
                    .mp3Url(audioUrl)
                    .songStatus(SongStatus.PENDING)
                    .build();

            Song savedSong = songRepository.save(song);

            // Step 6: Link Song with Artist
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new RuntimeException("Artist not found with ID: " + artistId));

            ArtistSong artistSong = new ArtistSong();
            artistSong.setArtistSongId(new ArtistSongId(artist, savedSong));
            artistSongRepository.save(artistSong);

            // Step 7: Link Song with Genres (if provided)
            if (genreNameList != null && !genreNameList.isEmpty()) {
                List<Genre> genres = genreRepository.findByGenresNameIn(genreNameList);
                List<GenreSong> genreSongs = genres.stream()
                        .map(genre -> new GenreSong(new GenreSongId(savedSong, genre)))
                        .collect(Collectors.toList());
                genreSongRepository.saveAll(genreSongs);
            } else {
                // Step 7B: Predict Genre
                String predictedGenreByLyric = null;
                String predictedGenreByAudio = null;

                if (lyrics != null && !lyrics.isBlank()) {
                    ResponseEntity<String> lyricPrediction = predictGenreByLyrics(lyrics);
                    FastApiResponse lyricGenreResult = extractResponseFromJson(lyricPrediction.getBody());
                    predictedGenreByLyric = lyricGenreResult.getGenre();
                }

                if (mp3File != null) {
                    ResponseEntity<String> audioPrediction = predictGenre(mp3File, requestId);
                    FastApiResponse audioGenreResult = extractResponseFromJson(audioPrediction.getBody());
                    predictedGenreByAudio = audioGenreResult.getGenre();
                }

                String finalGenre;
                String audioGenreNorm = predictedGenreByAudio != null ? predictedGenreByAudio.toLowerCase().replace("-", "_").replace(" ", "_") : null;
                String lyricGenreNorm = predictedGenreByLyric != null ? predictedGenreByLyric.toLowerCase().replace("-", "_").replace(" ", "_") : null;

                if (audioGenreNorm != null && audioGenreNorm.equals(lyricGenreNorm)) {
                    finalGenre = audioGenreNorm;
                } else {
                    finalGenre = audioGenreNorm != null ? audioGenreNorm : lyricGenreNorm;
                }

                if (finalGenre != null) {
                    Genre genre = genreRepository.findByGenresNameIgnoreCase(finalGenre)
                            .orElseThrow(() -> new RuntimeException("Predicted genre not found in database: " + finalGenre));

                    GenreSong genreSong = new GenreSong(new GenreSongId(savedSong, genre));
                    genreSongRepository.save(genreSong);
                }
            }

            // Step 8: Return success response
            return ApiResponse.ok("Upload successful!" +
                    (audioUrl != null ? " Audio URL: " + audioUrl : "") +
                    (imageUrl != null ? " | Image URL: " + imageUrl : "") +
                    " | Duration: " + formattedDuration);

        } catch (Exception e) {
            return ApiResponse.error("Đã xảy ra lỗi khi upload bài hát: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse updateSong(Long id, EditSongRequest request) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found with ID: " + id));

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
                            .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + index)))
                    .toList();

            List<GenreSong> genreSongs = genres.stream()
                    .map(genre -> new GenreSong(new GenreSongId(song, genre)))
                    .toList();

            song.setGenreSongs(genreSongs);
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = null;
            try {
                String imageOriginalFilename = request.getImage().getOriginalFilename();
                List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff");

                boolean isValidImage = allowedExtensions.stream()
                        .anyMatch(ext -> imageOriginalFilename.toLowerCase().endsWith(ext));

                if (!isValidImage) {
                    throw new IllegalArgumentException("Ảnh phải là một trong các định dạng: jpg, jpeg, png, gif, webp, bmp, tiff!");
                }

                imageUrl = uploadToCloudinary(request.getImage(), "image", "covers");

            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage(), e);
            }
            song.setImageUrl(imageUrl);
        }

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String mp3Url = null;
            String duration = null;
            try {
                // Validate audio file
                if (request.getFile() == null || request.getFile().isEmpty()) {
                    throw new IllegalArgumentException("File mp3 không được để trống!");
                }

                String originalFilename = request.getFile().getOriginalFilename();
                if (!originalFilename.toLowerCase().endsWith(".mp3")) {
                    throw new IllegalArgumentException("File phải có định dạng .mp3!");
                }

                // Upload audio to Cloudinary
                Map<?, ?> audioUploadResult = cloudinary.uploader().upload(
                        request.getFile().getBytes(),
                        ObjectUtils.asMap(
                                "resource_type", "video",
                                "public_id", "songs/" + UUID.randomUUID() + "_" + originalFilename
                        )
                );

                mp3Url = (String) audioUploadResult.get("secure_url");
                Double durationSeconds = (Double) audioUploadResult.get("duration");
                duration = formatDuration(durationSeconds);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi upload bài hát: " + e.getMessage(), e);
            }
            song.setMp3Url(mp3Url);
            song.setDuration(duration);
        }

        songRepository.save(song);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse deleteSong(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát với ID: " + id));

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
                .orElseThrow(() -> new RuntimeException("Song not found with ID: " + songId));

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
}
