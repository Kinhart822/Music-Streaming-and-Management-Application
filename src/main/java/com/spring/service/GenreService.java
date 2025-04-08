package com.spring.service;

import com.spring.dto.request.music.admin.GenreRequest;
import com.spring.dto.response.GenreResponse;

import java.util.List;

public interface GenreService {
    GenreResponse createGenre(GenreRequest request);
    GenreResponse updateGenre(Long id, GenreRequest request);
    void deleteGenre(Long id);
    GenreResponse getGenreById(Long id);
    List<GenreResponse> getAllGenres();
}
