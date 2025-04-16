package com.spring.dto.request.music;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AlbumRequest {
    @NotBlank
    private String name;

    private String description;
    private MultipartFile image;
}
