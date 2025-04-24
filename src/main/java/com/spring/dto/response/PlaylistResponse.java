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
    private Float playTimelength;
    private String releaseDate;
    private List<String> songNameList;
    private List<String> additionalArtistNameList;
    private String imageUrl;
    private String description;
    private String status;
}
