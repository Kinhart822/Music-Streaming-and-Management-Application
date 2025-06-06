package com.spring.dto.response;

import com.spring.constants.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArtistPresentation {
    private Long id;
    private String avatar;
    private String firstName;
    private String lastName;
    private String artistName;
    private String description;
    private String image;
    private Long numberOfFollowers;
    private String email;
    private String gender;
    private String birthDay;
    private String phone;
    private Integer status;
    private Long createdBy;
    private Long lastModifiedBy;
    private String createdDate;
    private String lastModifiedDate;
    private UserType userType;
    private List<Long> artistSongIds;
    private List<String> artistSongNameList;
    private List<Long> artistPlaylistIds;
    private List<String> artistPlaylistNameList;
    private List<Long> artistAlbumIds;
    private List<String> artistAlbumNameList;

}
