package com.spring.dto.request.music;

import lombok.Data;

import java.util.List;

@Data
public class AdminAddSongRequest {
    private String title;
    private String lyrics;
    private String duration;
    private List<Long> genreId;
    private List<Long> artistId;
}
