package com.wealthcopilot.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32) String secret,
        @Positive long expirationSeconds) {
}
