package com.spring.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FastApiResponse {
    private String requestId;
    private Boolean match;
    private Double similarityScore;
    private String genre;
    private String message;
}
