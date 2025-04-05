package com.spring.security;

import com.spring.constants.ApiResponseCode;
import com.spring.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class JwtHelper {
    private final JwtUtil jwtUtil;

    /**
     * Extracts the user ID from the Bearer token in the current HTTP request's Authorization header.
     *
     * @return the user ID associated with the token
     * @throws BusinessException BusinessException if the token is missing, invalid, or expired
     */
    public Long getIdUserRequesting() {
        HttpServletRequest hsRequest = getCurrentHttpRequest();
        if (hsRequest == null) {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }

        String authHeader = hsRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST_HEADER);
        }

        String accessToken = authHeader.substring(7);
        return jwtUtil.extractUserId(accessToken);
    }

    /**
     * Retrieves the current HttpServletRequest from the RequestContextHolder.
     *
     * @return the current HttpServletRequest
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }
}
