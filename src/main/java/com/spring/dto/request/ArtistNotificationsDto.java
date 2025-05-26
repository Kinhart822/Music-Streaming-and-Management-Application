package com.spring.dto.request;

import lombok.Data;

@Data
public class ArtistNotificationsDto {
    private Long notificationId;
    private Long targetArtistId;
}
