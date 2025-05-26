package com.spring.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private String title;
    private String content;
    private String createdDate;
}
