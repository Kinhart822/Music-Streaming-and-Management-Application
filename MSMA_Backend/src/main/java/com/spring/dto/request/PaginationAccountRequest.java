package com.spring.dto.request;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.UserType;
import com.spring.exceptions.BusinessException;
import lombok.Data;

@Data
public class PaginationAccountRequest {
    private int page = 1;
    private int size = 10;
    private String search = "";
    private Integer status;                // LOCKED(-4), DELETED(-3), INACTIVE(-1), ACTIVE(1);
    private String orderBy = "createdDate";
    private String userType;                // USER/ARTIST/ADMIN
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

    public UserType getParsedUserType() {
        if (userType == null || userType.isBlank()) {
            return null;
        }
        try {
            return UserType.valueOf(userType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }
    }
}



