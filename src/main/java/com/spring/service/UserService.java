package com.spring.service;

import com.spring.entities.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public interface UserService {
    UserDetailsService getUserDetailsCustomService();

    UserDetails loadUserByUsername(String username);

    User findByEmail(String email);
}

