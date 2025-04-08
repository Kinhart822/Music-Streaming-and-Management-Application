package com.spring.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class GenreResponse {
    private String name;
    private String imageUrl;
    private String description;
}
