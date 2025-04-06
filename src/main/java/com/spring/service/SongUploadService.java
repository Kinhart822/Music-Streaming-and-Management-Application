package com.spring.service;

import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;

public interface SongUploadService {
    ApiResponse uploadSong(SongUploadRequest songUploadRequest);
}
