package com.spring.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class GenreResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private String briefDescription;
    private String fullDescription;
    private String createdDate;
    private String lastModifiedDate;
}
