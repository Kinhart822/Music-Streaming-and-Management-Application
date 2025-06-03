package com.spring.dto.response;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class SongUploadResponse {
    private MultipartFile mp3File;
    private String mp3Url;
    private MultipartFile image;
    private String imageUrl;
    private String duration;
    private String title;
    private String lyrics;
    private Boolean downloadPermission;
    private String description;
    private List<String> genreNameList;
    private List<String> additionalArtistNameList;
}
