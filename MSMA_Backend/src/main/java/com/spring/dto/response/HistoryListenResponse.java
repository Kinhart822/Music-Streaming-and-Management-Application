package com.spring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryListenResponse {
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
    private String message;
}
