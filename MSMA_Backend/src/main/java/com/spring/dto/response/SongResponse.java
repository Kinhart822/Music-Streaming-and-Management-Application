package com.spring.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class SongResponse {
    private Long id;
    private String title;
    private String releaseDate;
    private String lyrics;
    private String duration;
    private String imageUrl;
    private Boolean downloadPermission;
    private String description;
    private String mp3Url;
    private String songStatus;
    private List<String> genreNameList;
    private List<String> artistNameList;
    private Long numberOfListeners;
    private Long countListen;
    private Long numberOfUserLike;
    private Long numberOfDownload;
}
