package com.spring.service;

import com.spring.entities.Song;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FastApiService {
    // TODO: Similarity
    ResponseEntity<String> checkLyricsSimilarity(String lyrics);
    ResponseEntity<String> checkAudioSimilarity(MultipartFile file);
    ResponseEntity<String> transcribeLyricsFromAudio(MultipartFile file);
    ResponseEntity<String> checkSimilarityBetweenLyricsAndAudio(String lyrics, String lyrics_audio);

    // TODO: Prediction
    ResponseEntity<String> lyricsPredictGenre(String lyrics);
    ResponseEntity<String> audioPredictGenre(MultipartFile file, String requestId);
    void handleGenres(Song song, List<String> genreNameList, String lyrics, MultipartFile mp3File, String requestId);
}
