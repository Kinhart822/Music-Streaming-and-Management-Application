package com.spring.service.impl;

import com.spring.dto.response.Otp;
import com.spring.entities.User;
import com.spring.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleMailMessage(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(fromEmail);
        javaMailSender.send(message);
    }

    @Override
    public void sendEmailVerificationOtp(Otp otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(otp.getEmail());
        message.setSubject("MSMA: Verify Your Email");
        String date = otp.getDueDate().toString().substring(0, 10);
        String time = otp.getDueDate().toString().substring(11, 16);
        message.setText(String.format("""
                Lmao!
                OTP: %s
                Please verify your email before %s %s
                """, otp.getOtp(), date, time));
        javaMailSender.send(message);
    }

    @Override
    public void sendResetPasswordMail(User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("MSMA: Reset Password");
        message.setText(String.format("""
                Lmao!
                OTP: %s
                """, user.getResetKey()));
        javaMailSender.send(message);
    }
}
