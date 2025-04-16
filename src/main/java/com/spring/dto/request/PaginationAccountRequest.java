package com.spring.dto.request;

import com.spring.constants.UserType;
import lombok.Data;

@Data
public class PaginationAccountRequest {
    private int page = 1;
    private int size = 10;
    private String search = "";
    private Integer status = 1;                // LOCKED(-4), DELETED(-3), INACTIVE(-1), ACTIVE(1);
    private String orderBy = "createdDate";
    private UserType userType;               // USER/ARTIST/ADMIN
    private String order = "asc";           // asc | desc

    public String getOrder() {
        return order != null ? order : "asc";
    }

    public String getSearch() {
        return search != null ? search : "";
    }

    public String getOrderBy() {
        return orderBy != null ? orderBy : "createdDate";
    }
}


