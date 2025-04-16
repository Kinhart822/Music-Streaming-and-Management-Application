package com.spring.dto.request.music;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class SongUploadRequest {
    private MultipartFile file;
    private String title;
    private String lyrics;
    private MultipartFile image;
    private Boolean downloadPermission;
    private String description;
    private List<Long> genreId;
}
