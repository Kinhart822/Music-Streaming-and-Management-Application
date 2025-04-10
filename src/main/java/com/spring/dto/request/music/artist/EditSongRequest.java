package com.spring.dto.request.music.artist;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class EditSongRequest {
    private String title;
    private String lyrics;
    private MultipartFile file;
    private MultipartFile image;
    private Boolean downloadPermission;
    private String description;
    private List<Long> genreId;
}
