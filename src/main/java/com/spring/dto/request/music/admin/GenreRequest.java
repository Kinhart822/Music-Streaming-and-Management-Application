package com.spring.dto.request.music.admin;

import lombok.Data;

@Data
public class GenreRequest {
    private String name;
    private String imageUrl;
    private String description;
}
