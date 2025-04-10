package com.spring.dto.request.music.artist;

import lombok.Data;

import java.util.List;

@Data
public class AddSongRequest {
    private Long playlistId;
    private Long albumId;
    private Long songId;
    private List<Long> songIdList;
}
