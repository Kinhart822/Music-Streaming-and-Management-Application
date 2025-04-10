package com.spring.service.impl;

import com.spring.config.EnvConfig;
import com.spring.constants.CommonStatus;
import com.spring.entities.User;
import com.spring.repository.UserRepository;
import com.spring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.apache.commons.text.RandomStringGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final int resetKeyTimeout = Integer.parseInt(Objects.requireNonNull(EnvConfig.get("RESET_KEY_TIMEOUT")));
    private static final RandomStringGenerator numericGenerator =
            new RandomStringGenerator.Builder()
                    .withinRange('0', '9') // Chỉ tạo số từ 0-9
                    .build();
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
    public Optional<User> resetPasswordRequest(String email) {
        String resetKey = numericGenerator.generate(6);
        while (userRepository.existsByResetKey(resetKey)) {
            resetKey = numericGenerator.generate(6);
        }
        String finalResetKey = resetKey;
        return userRepository
                .findByEmailIgnoreCaseAndStatus(email, CommonStatus.ACTIVE.getStatus())
                .map(u -> u.toBuilder()
                        .resetKey(finalResetKey)
                        .resetDueDate(Instant.now().plusSeconds(resetKeyTimeout))
                        .build());
    }

    @Override
    public Optional<User> resetPasswordCheck(String resetKey) {
        return userRepository
                .findByResetKeyAndStatusAndResetDueDateIsAfter(resetKey, CommonStatus.ACTIVE.getStatus(), Instant.now());
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    @Override
    public Optional<User> resetPasswordFinish(String resetKey, String encodedPassword) {
        return userRepository // TODO: Don't check for due
                .findByResetKeyAndStatusAndResetDueDateIsAfter(resetKey, CommonStatus.ACTIVE.getStatus(), Instant.now())
                .map (u -> u
                        .toBuilder()
                        .password(encodedPassword)
                        .resetKey(null)
                        .resetDueDate(null)
                        .build());
    }
}
