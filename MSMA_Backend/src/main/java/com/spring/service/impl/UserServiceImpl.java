package com.spring.service.impl;

import com.spring.entities.User;
import com.spring.repository.UserRepository;
import com.spring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserDetailsCustomServiceImpl userDetailsCustomServiceImpl;

    @Override
    public UserDetailsService getUserDetailsCustomService() {
        return userDetailsCustomServiceImpl;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return this.userDetailsCustomServiceImpl.loadUserByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }
}
