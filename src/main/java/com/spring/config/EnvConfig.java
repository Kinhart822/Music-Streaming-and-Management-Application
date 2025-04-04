package com.spring.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.load();  // Load một lần khi khởi động app

    public static String get(String key) {
        return dotenv.get(key);
    }
}

