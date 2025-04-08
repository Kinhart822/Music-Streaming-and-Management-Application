package com.spring.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.entities.Artist;
import com.spring.entities.ArtistSong;
import com.spring.entities.ArtistSongId;
import com.spring.entities.Song;
import com.spring.repository.ArtistRepository;
import com.spring.repository.ArtistSongRepository;
import com.spring.repository.SongRepository;
import com.spring.security.JwtHelper;
import com.spring.service.SongUploadService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional
public class SongUploadServiceImpl implements SongUploadService {
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final ArtistSongRepository artistSongRepository;
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

    @Override
    public ApiResponse uploadSong(SongUploadRequest songUploadRequest) {
        try {
            MultipartFile file = songUploadRequest.getFile();
            MultipartFile image = songUploadRequest.getImage();
            String title = songUploadRequest.getTitle();
            String lyrics = songUploadRequest.getLyrics();
            Boolean downloadPermission = songUploadRequest.getDownloadPermission();
            String description = songUploadRequest.getDescription();
            Long artistId = jwtHelper.getIdUserRequesting();

            if (file.isEmpty()) {
                return ApiResponse.error("File is empty");
            }

            String originalFilename = file.getOriginalFilename();
            if (!originalFilename.toLowerCase().endsWith(".mp3")) {
                return ApiResponse.error("Only .mp3 files are allowed");
            }

            // Check similarity


            // Upload audio to Cloudinary and get URL + duration
            Map audioUploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "video",
                            "public_id", "songs/" + UUID.randomUUID() + "_" + originalFilename
                    )
            );
            String audioUrl = (String) audioUploadResult.get("secure_url");
            Double durationSeconds = (Double) audioUploadResult.get("duration");
            String formattedDuration = formatDuration(durationSeconds);

            // Upload image to Cloudinary (if exists)
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                String imageOriginalFilename = image.getOriginalFilename();
                List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff");

                boolean isValidImage = allowedExtensions.stream()
                        .anyMatch(ext -> imageOriginalFilename.toLowerCase().endsWith(ext));

                if (!isValidImage) {
                    return ApiResponse.error("Only image files of type " + allowedExtensions + " are allowed");
                }

                imageUrl = uploadToCloudinary(image, "image","covers");
            }

            // Create and save song
            Song song = Song.builder()
                    .title(title)
                    .releaseDate(Date.from(Instant.now()))
                    .lyrics(lyrics)
                    .duration(formattedDuration) // format như "03:45"
                    .imageUrl(imageUrl)
                    .downloadPermission(downloadPermission != null && downloadPermission)
                    .description(description)
                    .countListen(0L)
                    .videoUrl(audioUrl)
                    .build();

            songRepository.save(song);

            // Link with artist
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new RuntimeException("Artist not found with ID: " + artistId));

            ArtistSongId artistSongId = new ArtistSongId();
            artistSongId.setArtist(artist);
            artistSongId.setSong(song);

            ArtistSong artistSong = new ArtistSong();
            artistSong.setArtistSongId(artistSongId);

            artistSongRepository.save(artistSong);

            return ApiResponse.ok("Upload successful! Audio URL: " + audioUrl +
                    (imageUrl != null ? " | Image URL: " + imageUrl : "") +
                    " | Duration: " + formattedDuration);
        } catch (Exception e) {
            return ApiResponse.error("Upload failed: " + e.getMessage());
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

    // Helper method to convert duration in seconds to "mm:ss"
    private String formatDuration(Double seconds) {
        if (seconds == null) return "00:00";
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

//    public ResponseEntity<String> predictGenre(@RequestParam("file") MultipartFile file) {
//        try {
//            // URL FastAPI
//            String fastApiUrl = "http://localhost:8000/predict-genre";
//
//            // Prepare file resource
//            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
//                @NotNull
//                @Override
//                public String getFilename() {
//                    return file.getOriginalFilename();
//                }
//            };
//
//            // Prepare multipart form
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//            body.add("audio_file", fileAsResource);
//
//            // Headers
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//            // Request
//            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//            RestTemplate restTemplate = new RestTemplate();
//
//            // Call FastAPI
//            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, requestEntity, String.class);
//
//            return ResponseEntity.ok(response.getBody());
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
//        }
//    }
//
//    public ResponseEntity<String> predictGenreByLyrics(@RequestParam("lyrics") String lyrics) {
//        try {
//            String fastApiUrl = "http://localhost:8000/predict-genre";
//
//            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
//            form.add("lyrics", lyrics);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
//            RestTemplate restTemplate = new RestTemplate();
//
//            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, request, String.class);
//            return ResponseEntity.ok(response.getBody());
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
//        }
//    }
//
//    public ResponseEntity<String> similarity(@RequestParam("file") MultipartFile file) {
//        try {
//            // URL FastAPI
//            String fastApiUrl = "http://localhost:8000/check-similarity";
//
//            // Prepare file resource
//            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
//                @NotNull
//                @Override
//                public String getFilename() {
//                    return file.getOriginalFilename();
//                }
//            };
//
//            // Prepare multipart form
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//            body.add("audio_file", fileAsResource);
//
//            // Headers
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//            // Request
//            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//            RestTemplate restTemplate = new RestTemplate();
//
//            // Call FastAPI
//            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, requestEntity, String.class);
//
//            return ResponseEntity.ok(response.getBody());
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
//        }
//    }
//
//    public ResponseEntity<String> similarityByLyrics(@RequestParam("lyrics") String lyrics) {
//        try {
//            String fastApiUrl = "http://localhost:8000/check-similarity";
//
//            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
//            form.add("lyrics", lyrics);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
//            RestTemplate restTemplate = new RestTemplate();
//
//            ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, request, String.class);
//            return ResponseEntity.ok(response.getBody());
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
//        }
//    }
}
