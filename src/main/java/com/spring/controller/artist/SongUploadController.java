package com.spring.controller.artist;

import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.service.SongUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/artist")
@RequiredArgsConstructor
public class SongUploadController {
    private final SongUploadService songUploadService;

    @PostMapping("/song/uploadFile")
    public ResponseEntity<ApiResponse> uploadSong(@ModelAttribute @Valid SongUploadRequest songUploadRequest) {
        return ResponseEntity.ok(songUploadService.uploadSong(songUploadRequest));
    }

    @PostMapping("/song/predict-genre")
    public ResponseEntity<String> predictGenre(@RequestParam("file") MultipartFile file) {
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

    @PostMapping("/song/predict-genre-by-lyrics")
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

    @PostMapping("/song/check-similarity")
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

    @PostMapping("/song/check-similarity-by-lyrics")
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

}

