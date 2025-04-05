package com.spring.service;

import com.spring.dto.response.Otp;
import com.spring.entities.User;

public interface EmailService {
    void sendSimpleMailMessage(String toEmail, String subject, String body);
    void sendEmailVerificationOtp(Otp otp);
    void sendResetPasswordMail(User user);
}
