package com.spring.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spring.dto.request.music.admin.GenreRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.GenreResponse;
import com.spring.entities.Genre;
import com.spring.entities.GenreSong;
import com.spring.repository.GenreRepository;
import com.spring.repository.GenreSongRepository;
import com.spring.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;
    private final Cloudinary cloudinary;

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

    private String imageUpload(MultipartFile imageUpload) {
        try {
            if (imageUpload != null && !imageUpload.isEmpty()) {
                String imageOriginalFilename = imageUpload.getOriginalFilename();
                List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff");

                boolean isValidImage = allowedExtensions.stream()
                        .anyMatch(ext -> imageOriginalFilename.toLowerCase().endsWith(ext));

                if (!isValidImage) {
                    throw new IllegalArgumentException("Ảnh phải là một trong các định dạng: jpg, jpeg, png, gif, webp, bmp, tiff!");
                }

                return uploadToCloudinary(imageUpload, "image", "covers");
            }

            return null; // if imageUpload is null or empty
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage(), e);
        }
    }

    @Override
    public GenreResponse createGenre(GenreRequest request) {
        String imageUrl = imageUpload(request.getImage());

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
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id));

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
            genre.setImageUrl(imageUpload(request.getImage()));
        }

        genreRepository.save(genre);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse deleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id));

        genreRepository.delete(genre);

        return ApiResponse.ok("Xoá thành công!");
    }

    @Override
    public GenreResponse getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found with ID: " + id));

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
