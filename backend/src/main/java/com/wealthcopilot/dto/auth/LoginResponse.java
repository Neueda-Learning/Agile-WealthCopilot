package com.wealthcopilot.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
