package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.dto.request.music.GenreRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.GenreResponse;
import com.spring.entities.Genre;
import com.spring.exceptions.BusinessException;
import com.spring.repository.GenreRepository;
import com.spring.service.CloudinaryService;
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
    private final GenreRepository genreRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public GenreResponse createGenre(GenreRequest request) {
        String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getImage());

        Genre genre = Genre.builder()
                .genresName(request.getName())
                .imageUrl(imageUrl)
                .briefDescription(request.getBriefDescription())
                .fullDescription(request.getFullDescription())
                .build();
        genreRepository.save(genre);

        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .briefDescription(genre.getBriefDescription())
                .fullDescription(genre.getFullDescription())
                .build();
    }

    @Override
    public ApiResponse updateGenre(Long id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            genre.setGenresName(request.getName());
        }

        if (request.getBriefDescription() != null && !request.getBriefDescription().isBlank()) {
            genre.setBriefDescription(request.getBriefDescription());
        }

        if (request.getFullDescription() != null && !request.getFullDescription().isBlank()) {
            genre.setFullDescription(request.getFullDescription());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            genre.setImageUrl(cloudinaryService.uploadImageToCloudinary(request.getImage()));
        }

        genreRepository.save(genre);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse deleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        genreRepository.delete(genre);

        return ApiResponse.ok("Xoá thành công!");
    }

    @Override
    public GenreResponse getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getGenresName())
                .imageUrl(genre.getImageUrl())
                .briefDescription(genre.getBriefDescription())
                .fullDescription(genre.getFullDescription())
                .build();
    }

    @Override
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genre -> GenreResponse.builder()
                        .id(genre.getId())
                        .name(genre.getGenresName())
                        .imageUrl(genre.getImageUrl())
                        .briefDescription(genre.getBriefDescription())
                        .fullDescription(genre.getFullDescription())
                        .build())
                .collect(Collectors.toList());
    }

}
