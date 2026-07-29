package com.wealthcopilot;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class WealthCopilotApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(WealthCopilotApplication.class, args);
    }

    /**
     * Lets local Maven runs use the repository's .env without shell setup.
     * Real environment variables and explicit JVM properties always win.
     */
    private static void loadDotenv() {
        Dotenv.configure()
                .directory("../")
                .ignoreIfMissing()
                .load()
                .entries()
                .forEach(entry -> {
                    String key = entry.getKey();
                    if (System.getenv(key) == null && System.getProperty(key) == null) {
                        System.setProperty(key, entry.getValue());
                    }
                });
    }
}
