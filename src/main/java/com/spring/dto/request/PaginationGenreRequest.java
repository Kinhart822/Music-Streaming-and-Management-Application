package com.spring.dto.request;

import lombok.Data;

@Data
public class PaginationGenreRequest {
    private int page = 1;
    private int size = 10;
    private String search = "";

    public String getSearch() {
        return search != null ? search : "";
    }
}


