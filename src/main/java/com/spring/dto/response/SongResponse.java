package com.spring.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class SongResponse {
    private Long id;
    private String title;
    private String releaseDate;
    private String lyrics;
    private String duration;
    private String imageUrl;    // Song image url from Cloudinary
    private String artSmallUrl;
    private String artMediumUrl;
    private String artBigUrl;
    private Boolean downloadPermission;
    private String description;
    private String mp3Url;        // Song file url from Cloudinary
    private String trackUrl;       // Song url from Spotify_API
    private String songStatus;
}
