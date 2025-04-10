package com.spring.security;

import com.spring.constants.CommonStatus;
import com.spring.entities.User;
import com.spring.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserStatusFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userService.findByEmail(email);

            if (user.getStatus().equals(CommonStatus.DELETED.getStatus())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản User đã bị xoá.");
                return;
            }

            if (user.getStatus().equals(CommonStatus.INACTIVE.getStatus())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản Artist đã bị vô hiệu hoá.");
                return;
            }

            if (user.getStatus().equals(CommonStatus.LOCKED.getStatus())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản Admin đã bị khoá.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
