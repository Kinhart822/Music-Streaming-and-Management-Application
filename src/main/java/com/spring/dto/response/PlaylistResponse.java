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
public class PlaylistResponse {
    private Long id;
    private String playlistName;
    private Float playTimeLength;
    private String releaseDate;
    private List<Long> songIdList;
    private List<String> songNameList;
    private List<Long> artistIdList;
    private List<String> artistNameList;
    private String imageUrl;
    private String description;
    private String status;
}
