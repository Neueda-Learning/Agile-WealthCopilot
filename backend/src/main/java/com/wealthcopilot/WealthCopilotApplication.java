package com.wealthcopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WealthCopilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WealthCopilotApplication.class, args);
    }
}
