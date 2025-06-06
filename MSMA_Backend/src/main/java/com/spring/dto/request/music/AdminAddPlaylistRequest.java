package com.spring.dto.request.music;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AdminAddPlaylistRequest {
    @NotBlank
    private String playlistName;

    private String description;
    private List<Long> songIds;
    private List<Long> artistIds;
    private MultipartFile image;
}
