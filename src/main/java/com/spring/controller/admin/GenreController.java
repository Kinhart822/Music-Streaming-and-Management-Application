package com.spring.controller.admin;

import com.spring.dto.request.music.admin.GenreRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.GenreResponse;
import com.spring.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/genre")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;

    @PostMapping("/create")
    public ResponseEntity<GenreResponse> createGenre(@ModelAttribute @Valid GenreRequest request) {
        return ResponseEntity.ok(genreService.createGenre(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateGenre(@PathVariable Long id, @ModelAttribute @Valid GenreRequest request) {
        return ResponseEntity.ok(genreService.updateGenre(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteGenre(@PathVariable Long id) {
        return ResponseEntity.ok(genreService.deleteGenre(id));
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable Long id) {
        GenreResponse response = genreService.getGenreById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/infoAll")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        List<GenreResponse> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
    }
}
