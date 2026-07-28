package com.wealthcopilot.security;

import com.wealthcopilot.entity.ApiKey;
import com.wealthcopilot.repository.ApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final Clock clock;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, Clock clock) {
        this.apiKeyRepository = apiKeyRepository;
        this.clock = clock;
    }

    @Transactional
    public boolean authenticate(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            return false;
        }
        return apiKeyRepository.findByKeyHashAndRevokedAtIsNull(hash(plaintextKey))
                .map(apiKey -> {
                    apiKey.setLastUsedAt(LocalDateTime.now(clock));
                    apiKeyRepository.save(apiKey);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void bootstrap(String plaintextKey, String label) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            return;
        }
        String keyHash = hash(plaintextKey);
        if (apiKeyRepository.existsByKeyHash(keyHash)) {
            return;
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setLabel(label == null || label.isBlank() ? "Instructor demo key" : label.trim());
        apiKeyRepository.save(apiKey);
    }

    String hash(String plaintextKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(plaintextKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
