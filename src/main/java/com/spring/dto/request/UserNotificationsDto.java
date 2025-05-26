package com.spring.dto.request;

import lombok.Data;

@Data
public class UserNotificationsDto {
    private Long notificationId;
    private Long followedArtistId;
    private Long targetUserId;
}
