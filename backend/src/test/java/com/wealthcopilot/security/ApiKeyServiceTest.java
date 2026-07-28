package com.wealthcopilot.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wealthcopilot.entity.ApiKey;
import com.wealthcopilot.repository.ApiKeyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC);
        apiKeyService = new ApiKeyService(apiKeyRepository, clock);
    }

    @Test
    void authenticate_hashesKeyAndUpdatesLastUsedTime() {
        ApiKey apiKey = new ApiKey();
        String expectedHash = "9f86d081884c7d659a2feaa0c55ad015"
                + "a3bf4f1b2b0b822cd15d6c15b0f00a08";
        when(apiKeyRepository.findByKeyHashAndRevokedAtIsNull(expectedHash)).thenReturn(Optional.of(apiKey));

        assertTrue(apiKeyService.authenticate("test"));
        assertEquals(LocalDateTime.parse("2026-07-28T12:00:00"), apiKey.getLastUsedAt());
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void authenticate_rejectsMissingKeyWithoutDatabaseLookup() {
        assertFalse(apiKeyService.authenticate(" "));
        verify(apiKeyRepository, never()).findByKeyHashAndRevokedAtIsNull(any());
    }

    @Test
    void bootstrap_storesOnlyHashAndIsIdempotent() {
        when(apiKeyRepository.existsByKeyHash(any())).thenReturn(false);

        apiKeyService.bootstrap("plaintext-secret", "Instructor");

        verify(apiKeyRepository).save(any(ApiKey.class));
    }
}
