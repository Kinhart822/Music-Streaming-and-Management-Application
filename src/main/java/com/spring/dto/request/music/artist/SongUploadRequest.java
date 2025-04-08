package com.spring.dto.request.music.artist;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SongUploadRequest {
    private MultipartFile file;
    private String title;
    private String lyrics;
    private MultipartFile image;
    private Boolean downloadPermission;
    private String description;
//    private Long genreId;
}
