package com.spring.service;

import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.HistoryListenResponse;

import java.util.List;

public interface UserSongCountService {
    ApiResponse incrementListenCount(Long id);
    List<HistoryListenResponse> getAllHistoryListenByCurrentUser();
    List<HistoryListenResponse> getAllRecentListeningCurrentUser();
}
