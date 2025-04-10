package com.spring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlbumResponse {
    private Long id;
    private String name;
    private String description;
    private String releaseDate;
    private Float albumTimeLength;
    private String imageUrl;
    private String status;
}
