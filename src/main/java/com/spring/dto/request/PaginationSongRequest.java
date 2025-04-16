package com.spring.dto.request;

import lombok.Data;

@Data
public class PaginationSongRequest {
    private int page = 1;
    private int size = 10;
    private Long genreId;                    // Lọc theo thể loại
    private String orderBy = "releaseDate";  // releaseDate | songCount
    private String order = "asc";            // asc | desc
    private String search = "";

    public String getOrder() {
        return order != null ? order : "asc";
    }

    public String getSearch() {
        return search != null ? search : "";
    }

    public String getOrderBy() {
        return orderBy != null ? orderBy : "releaseDate";
    }
}


