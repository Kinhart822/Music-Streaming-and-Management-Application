package com.spring.dto.request.account;

import lombok.Data;

import java.util.List;

@Data
public class CreateArtistFromList {
    private List<CreateArtist> artistList;
}
