package com.spring.dto.request.music;

import lombok.Data;

import java.util.List;

@Data
public class RemoveSongRequest {
    private Long playlistId;
    private Long albumId;
    private Long songId;
    private List<Long> songIdList;
}
