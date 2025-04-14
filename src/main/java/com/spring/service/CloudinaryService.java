package com.spring.service;

import com.spring.dto.response.SongUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadImageToCloudinary(MultipartFile image);
    SongUploadResponse uploadAudioToCloudinary(MultipartFile file);
}
