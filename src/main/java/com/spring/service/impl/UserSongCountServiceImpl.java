package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.HistoryListenResponse;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.HistoryListenRepository;
import com.spring.repository.SongRepository;
import com.spring.repository.UserRepository;
import com.spring.repository.UserSongCountRepository;
import com.spring.security.JwtHelper;
import com.spring.service.UserSongCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSongCountServiceImpl implements UserSongCountService {
    private final UserSongCountRepository userSongCountRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final HistoryListenRepository historyListenRepository;
    private final JwtHelper jwtHelper;

    @Override
    public ApiResponse incrementListenCount(Long songId) {
        Long userId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        UserSongCountId countId = new UserSongCountId(song, user);

        UserSongCount userSongCount = userSongCountRepository.findById(countId)
                .orElse(UserSongCount.builder()
                        .userSongCountId(countId)
                        .countListen(0L)
                        .build());

        boolean isFirstListen = userSongCount.getCountListen() == 0;

        userSongCount.setCountListen(userSongCount.getCountListen() + 1);
        userSongCountRepository.save(userSongCount);

        if (isFirstListen) {
            song.setCountListener(song.getCountListener() != null ? song.getCountListener() + 1 : 1);
            songRepository.save(song);
        }

        HistoryListen historyListen = HistoryListen.builder()
                .user(user)
                .song(song)
                .dateTime(LocalDateTime.now())
                .build();
        historyListenRepository.save(historyListen);

        return ApiResponse.ok("Listen count incremented successfully");
    }

    @Override
    public List<HistoryListenResponse> getAllHistoryListenByCurrentUser() {
        Long currentUserId = jwtHelper.getIdUserRequesting();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Lấy tất cả lịch sử người dùng hiện tại
        return historyListenRepository.findAll().stream()
                .filter(hs -> hs.getUser() != null && hs.getUser().getId().equals(currentUserId))
                // Nhóm theo songId, lấy bản ghi có thời gian mới nhất
                .collect(Collectors.toMap(
                        hs -> hs.getSong().getId(), // key: songId
                        hs -> hs,                   // value: bản ghi ban đầu
                        (hs1, hs2) -> hs1.getDateTime().isAfter(hs2.getDateTime()) ? hs1 : hs2 // lấy bản mới hơn
                ))
                .values().stream()
                // Sắp xếp lại theo thời gian mới nhất trước
                .sorted((a, b) -> b.getDateTime().compareTo(a.getDateTime()))
                .map(hs -> {
                    String formattedDate = hs.getDateTime().format(formatter);
                    String message = "🎧 Last played on " + formattedDate;
                    return HistoryListenResponse.builder()
                            .imageUrl(hs.getSong().getImageUrl())
                            .songName(hs.getSong().getTitle())
                            .message(message)
                            .build();
                })
                .toList();

    }
}
