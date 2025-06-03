package com.spring.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FastApiResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String lyrics;   // Audio Lyrics
    private Boolean match;
    private Boolean isNotMatch;

    @JsonProperty("similarity_score")
    private Double similarityScore;

    private String genre;
    private String message;
}
