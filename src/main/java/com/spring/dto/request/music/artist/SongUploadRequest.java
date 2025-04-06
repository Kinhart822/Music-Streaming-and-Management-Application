package com.spring.dto.request.music.artist;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SongUploadRequest {
    private MultipartFile file; // Bạn vẫn cần sử dụng MultipartFile cho file upload
    private String title;
    private Long artistId;
}
