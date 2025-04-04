package com.spring.security;

import com.spring.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest hsRequest, @NotNull HttpServletResponse hsResponse, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        for (String element : Arrays
                .stream(ApiEndpoints.PERMITTED.getApis())
                .map(s -> {
                    if (s.endsWith("/**")) s = s.substring(0, s.length() - 3);
                    return s;
                })
                .toList()) {
            if (hsRequest.getRequestURI().contains(element)) {
                filterChain.doFilter(hsRequest, hsResponse);
                return;
            }
        }
        System.out.println(hsRequest.getRequestURI());
        String authHeader = hsRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            unauthorizedRequest(hsResponse);
            return;
        }
        try {
            String accessToken = authHeader.substring(7);
            // Check if the token contains a subject
            String username = jwtUtil.extractUsername(accessToken);
            if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                unauthorizedRequest(hsResponse);
                return;
            }

            UserDetails userDetails = userService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(hsRequest));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(hsRequest, hsResponse);
        } catch (ExpiredJwtException e) {
            // Token is expired
            unauthorizedRequest(hsResponse);
        }
    }

    private void unauthorizedRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Unauthorized: Invalid or missing token");
    }
}
