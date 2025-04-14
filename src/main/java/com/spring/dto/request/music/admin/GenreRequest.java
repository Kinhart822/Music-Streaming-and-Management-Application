package com.spring.dto.request.music.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class GenreRequest {
    @NotBlank
    private String name;

    private MultipartFile image;
    private String briefDescription;
    private String fullDescription;
}
