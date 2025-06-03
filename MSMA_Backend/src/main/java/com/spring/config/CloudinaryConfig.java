package com.spring.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    private static final String CLOUDINARY_CLOUD_NAME = EnvConfig.get("CLOUDINARY_CLOUD_NAME");
    private static final String CLOUDINARY_API_KEY = EnvConfig.get("CLOUDINARY_API_KEY");
    private static final String CLOUDINARY_API_SECRET = EnvConfig.get("CLOUDINARY_API_SECRET");

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
        config.put("api_key", CLOUDINARY_API_KEY);
        config.put("api_secret", CLOUDINARY_API_SECRET);
        config.put("secure", "true");
        return new Cloudinary(config);
    }
}
