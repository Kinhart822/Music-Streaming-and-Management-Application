package com.spring;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class MsmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsmaApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(JdbcConnectionDetails jdbc) {
        return args -> {
            System.out.println(
                    "class: " + jdbc.getClass().getName() + "\n" +
                    "JDBC URL: " + jdbc.getJdbcUrl() + "\n" +
                    "Username: " + jdbc.getUsername() + "\n" +
                    "Password: " + jdbc.getPassword()
            );
        };
    }
}
