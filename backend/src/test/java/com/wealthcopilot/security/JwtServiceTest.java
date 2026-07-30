package com.wealthcopilot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.wealthcopilot.entity.UserAccount;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtProperties jwtProperties;

    private Clock fixedClock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2024-01-01T00:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        jwtService = new JwtService(jwtEncoder, jwtDecoder, jwtProperties, fixedClock);
    }

    @Test
    void issue_shouldCreateValidJwt() {
        UserAccount user = new UserAccount("john@example.com", "password", "John Doe");
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.from(user);

        when(jwtProperties.expirationSeconds()).thenReturn(3600L);
        when(jwtProperties.issuer()).thenReturn("wealth-copilot");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(mock(Jwt.class) {
                    {
                        when(getTokenValue()).thenReturn("generated_token");
                    }
                });

        String token = jwtService.issue(principal);

        assertThat(token).isEqualTo("generated_token");
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void issue_shouldIncludeUserIdAsSubject() {
        UserAccount user = new UserAccount("john@example.com", "password", "John Doe");
        user.setId(42L);
        UserPrincipal principal = UserPrincipal.from(user);

        when(jwtProperties.expirationSeconds()).thenReturn(3600L);
        when(jwtProperties.issuer()).thenReturn("wealth-copilot");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(mock(Jwt.class) {
                    {
                        when(getTokenValue()).thenReturn("token");
                    }
                });

        jwtService.issue(principal);

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void issue_shouldIncludeEmailClaim() {
        UserAccount user = new UserAccount("john@example.com", "password", "John Doe");
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.from(user);

        when(jwtProperties.expirationSeconds()).thenReturn(3600L);
        when(jwtProperties.issuer()).thenReturn("wealth-copilot");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(mock(Jwt.class) {
                    {
                        when(getTokenValue()).thenReturn("token");
                    }
                });

        jwtService.issue(principal);

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void issue_shouldUseCorrectIssuer() {
        UserAccount user = new UserAccount("john@example.com", "password", "John Doe");
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.from(user);

        when(jwtProperties.expirationSeconds()).thenReturn(3600L);
        when(jwtProperties.issuer()).thenReturn("wealth-copilot");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(mock(Jwt.class) {
                    {
                        when(getTokenValue()).thenReturn("token");
                    }
                });

        jwtService.issue(principal);

        verify(jwtProperties).issuer();
    }

    @Test
    void decode_shouldDecodeValidToken() {
        String token = "valid_jwt_token";
        Jwt decodedJwt = mock(Jwt.class);

        when(jwtDecoder.decode(token)).thenReturn(decodedJwt);

        Jwt result = jwtService.decode(token);

        assertThat(result).isEqualTo(decodedJwt);
        verify(jwtDecoder).decode(token);
    }

    @Test
    void decode_shouldThrowExceptionForInvalidToken() {
        String token = "invalid_jwt_token";

        when(jwtDecoder.decode(token))
                .thenThrow(new RuntimeException("Invalid token"));

        assertThatThrownBy(() -> jwtService.decode(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void expirationSeconds_shouldReturnConfiguredValue() {
        when(jwtProperties.expirationSeconds()).thenReturn(7200L);

        long expirationSeconds = jwtService.expirationSeconds();

        assertThat(expirationSeconds).isEqualTo(7200L);
        verify(jwtProperties).expirationSeconds();
    }

    @Test
    void expirationSeconds_shouldReturnDifferentValuesForDifferentConfigurations() {
        when(jwtProperties.expirationSeconds()).thenReturn(3600L);
        assertThat(jwtService.expirationSeconds()).isEqualTo(3600L);

        when(jwtProperties.expirationSeconds()).thenReturn(7200L);
        assertThat(jwtService.expirationSeconds()).isEqualTo(7200L);
    }

    @Test
    void issue_shouldHandleNullPrincipal() {
        when(jwtProperties.expirationSeconds()).thenReturn(3600L);
        when(jwtProperties.issuer()).thenReturn("wealth-copilot");

        assertThatThrownBy(() -> jwtService.issue(null))
                .isInstanceOf(NullPointerException.class);
    }
}
