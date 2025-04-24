package com.spring.dto.request.music;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class SongUploadRequest {
    private String title;
    private List<Long> genreIds = new ArrayList<>();
    private String lyrics;
    private String description;
    private List<Long> additionalArtistIds = new ArrayList<>();
    private MultipartFile file;
    private MultipartFile image;
    private Boolean downloadPermission;
}
