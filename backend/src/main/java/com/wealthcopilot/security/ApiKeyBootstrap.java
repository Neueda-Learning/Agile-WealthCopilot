package com.wealthcopilot.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyBootstrap implements ApplicationRunner {

    private final ApiKeyService apiKeyService;
    private final ExternalApiProperties properties;

    public ApiKeyBootstrap(ApiKeyService apiKeyService, ExternalApiProperties properties) {
        this.apiKeyService = apiKeyService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        apiKeyService.bootstrap(properties.getBootstrapKey(), properties.getBootstrapLabel());
    }
}
