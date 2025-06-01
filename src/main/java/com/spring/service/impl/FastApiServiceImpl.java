package com.spring.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.constants.ApiResponseCode;
import com.spring.dto.response.FastApiResponse;
import com.spring.entities.Genre;
import com.spring.entities.GenreSong;
import com.spring.entities.GenreSongId;
import com.spring.entities.Song;
import com.spring.exceptions.BusinessException;
import com.spring.repository.GenreRepository;
import com.spring.repository.GenreSongRepository;
import com.spring.service.FastApiService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FastApiServiceImpl implements FastApiService {
    private final GenreRepository genreRepository;
    private final GenreSongRepository genreSongRepository;
    private static final String BASE_URL = "http://fastapi-container:8000";
//    private static final String BASE_URL = "http://host.docker.internal:8000";


    private FastApiResponse extractResponseFromJson(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(responseBody, FastApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi phân tích JSON: " + e.getMessage(), e);
        }
    }

    private String chooseFinalGenre(String lyricGenre, String audioGenre) {
        String normalizeLyric = lyricGenre != null ? lyricGenre.toLowerCase().replace("-", "_").replace(" ", "_") : null;
        String normalizeAudio = audioGenre != null ? audioGenre.toLowerCase().replace("-", "_").replace(" ", "_") : null;

        if (normalizeAudio != null && normalizeAudio.equals(normalizeLyric)) return normalizeAudio;
        return normalizeAudio != null ? normalizeAudio : normalizeLyric;
    }

    @Override
    public ResponseEntity<String> checkLyricsSimilarity(String lyrics) {
        String url = BASE_URL + "/check-similarity";

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("lyrics", lyrics);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> checkAudioSimilarity(MultipartFile file) {
        String url = BASE_URL + "/check-similarity";

        try {
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
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> transcribeLyricsFromAudio(MultipartFile file) {
        String url = BASE_URL + "/transcribe-lyrics";

        try {
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
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> checkSimilarityBetweenLyricsAndAudio(String lyrics, String lyrics_audio) {
        String url = BASE_URL + "/check-similar-between-input-and-audio";
        try {
            // Prepare multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("lyrics", lyrics);
            body.add("lyrics_audio", lyrics_audio);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> lyricsPredictGenre(String lyrics) {
        String url = BASE_URL + "/predict-genre";

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("lyrics", lyrics);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> audioPredictGenre(MultipartFile file, String requestId) {
        String url = BASE_URL + "/predict-genre";

        try {
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
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void handleGenres(Song song, List<String> genreNameList, String lyrics, MultipartFile mp3File, String requestId) {
        if (genreNameList != null && !genreNameList.isEmpty()) {
            List<Genre> genres = genreRepository.findByGenresNameIn(genreNameList);
            List<GenreSong> genreSongs = genres.stream()
                    .map(genre -> new GenreSong(new GenreSongId(song, genre)))
                    .collect(Collectors.toList());
            genreSongRepository.saveAll(genreSongs);
        }

        String genreByLyrics = null;
        String genreByAudio = null;

        if (lyrics != null && !lyrics.isBlank()) {
            ResponseEntity<String> response = lyricsPredictGenre(lyrics);
            FastApiResponse result = extractResponseFromJson(response.getBody());
            genreByLyrics = result != null ? result.getGenre() : null;
        }

        if (mp3File != null && !mp3File.isEmpty()) {
            ResponseEntity<String> response = audioPredictGenre(mp3File, requestId);
            FastApiResponse result = extractResponseFromJson(response.getBody());
            genreByAudio = result != null ? result.getGenre() : null;
        }

        // Normalize
        String finalGenre = chooseFinalGenre(genreByLyrics, genreByAudio);
        if (finalGenre != null) {
            Genre genre = genreRepository.findByGenresNameIgnoreCase(finalGenre)
                    .orElseThrow(() -> new RuntimeException("Predicted genre not found in database: " + finalGenre));
            genreSongRepository.save(new GenreSong(new GenreSongId(song, genre)));
        }
    }
}
