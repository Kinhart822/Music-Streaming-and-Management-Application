package com.spring.dto.request.music;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class SongUploadRequest {
    private String title;
    private List<Long> genreIds;
    private String lyrics;
    private String description;
    private List<Long> artistIds;
    private MultipartFile file;
    private MultipartFile image;
    private Boolean downloadPermission;
}
