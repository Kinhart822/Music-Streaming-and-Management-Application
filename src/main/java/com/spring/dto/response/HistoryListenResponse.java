package com.spring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryListenResponse {
    private String imageUrl;
    private String songName;
    private String message;
}
