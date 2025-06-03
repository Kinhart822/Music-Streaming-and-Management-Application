package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.CommonStatus;
import com.spring.exceptions.BusinessException;
import com.spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * encapsulated in user service
 */
@Service
@RequiredArgsConstructor
public class UserDetailsCustomServiceImpl implements UserDetailsService{
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository
                .findByEmailIgnoreCaseAndStatus(username, CommonStatus.ACTIVE.getStatus())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.USERNAME_NOT_EXISTED_OR_DEACTIVATED));
    }
}
