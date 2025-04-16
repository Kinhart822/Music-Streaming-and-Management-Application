package com.spring.service;

import com.spring.dto.response.ApiResponse;

public interface UserSongCountService {
    ApiResponse incrementListenCount(Long id);
}
