package com.spring.service;

import com.spring.dto.request.music.GenreRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.GenreResponse;

import java.util.List;

public interface GenreService {
    GenreResponse createGenre(GenreRequest request);
    ApiResponse updateGenre(Long id, GenreRequest request);
    ApiResponse deleteGenre(Long id);
    GenreResponse getGenreById(Long id);
    List<GenreResponse> getAllGenres();
}
