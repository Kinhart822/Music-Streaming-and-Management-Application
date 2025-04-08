package com.spring.service.impl;

import com.spring.dto.request.music.admin.GenreRequest;
import com.spring.dto.response.GenreResponse;
import com.spring.entities.Genre;
import com.spring.repository.GenresRepository;
import com.spring.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {
    private final GenresRepository genresRepository;

    @Override
    public GenreResponse createGenre(GenreRequest request) {
        Genre genre = Genre.builder()
                .genresName(request.getName())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .countListen(0L)
                .build();
        genresRepository.save(genre);

        return GenreResponse.builder()
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .description(genre.getDescription())
                .build();
    }

    @Override
    public GenreResponse updateGenre(Long id, GenreRequest request) {
        Genre genre = genresRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id));

        genre.setGenresName(request.getName());
        genre.setImageUrl(request.getImageUrl());
        genre.setDescription(request.getDescription());

        return GenreResponse.builder()
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .description(genre.getDescription())
                .build();
    }


    @Override
    public void deleteGenre(Long id) {
        Genre genre = genresRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id));
        genresRepository.delete(genre);
    }

    @Override
    public GenreResponse getGenreById(Long id) {
        Genre genre = genresRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id));

        return GenreResponse.builder()
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .description(genre.getDescription())
                .build();
    }

    @Override
    public List<GenreResponse> getAllGenres() {
        return genresRepository.findAll().stream()
                .map(genre -> GenreResponse.builder()
                        .name(genre.getGenresName())
                        .imageUrl(genre.getImageUrl())
                        .description(genre.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

}
