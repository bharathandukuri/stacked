package com.bharath.stacked;

import com.bharath.stacked.config.DotenvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StackedApplication {

    public static void main(String[] args) {
        // Pre-load .env into system properties before Spring initializes
        DotenvConfig.loadDotenv();
        SpringApplication.run(StackedApplication.class, args);
    }

}
