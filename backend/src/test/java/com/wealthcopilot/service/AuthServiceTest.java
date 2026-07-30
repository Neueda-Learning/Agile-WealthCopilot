package com.wealthcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wealthcopilot.dto.auth.LoginRequest;
import com.wealthcopilot.dto.auth.LoginResponse;
import com.wealthcopilot.dto.auth.RegisterRequest;
import com.wealthcopilot.dto.auth.UserResponse;
import com.wealthcopilot.entity.UserAccount;
import com.wealthcopilot.exception.ConflictException;
import com.wealthcopilot.exception.InvalidCredentialsException;
import com.wealthcopilot.repository.UserAccountRepository;
import com.wealthcopilot.security.JwtService;
import com.wealthcopilot.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService);
    }

    @Test
    void register_shouldCreateNewUser() {
        RegisterRequest request = new RegisterRequest("john@example.com", "password123", "John Doe");
        UserAccount savedUser = new UserAccount("john@example.com", "encoded_password", "John Doe");
        savedUser.setId(1L);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.saveAndFlush(any(UserAccount.class))).thenReturn(savedUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john@example.com");
        verify(userRepository).existsByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).saveAndFlush(any(UserAccount.class));
    }

    @Test
    void register_shouldNormalizeEmail() {
        RegisterRequest request = new RegisterRequest("JOHN@EXAMPLE.COM", "password123", "John Doe");
        UserAccount savedUser = new UserAccount("john@example.com", "encoded_password", "John Doe");
        savedUser.setId(1L);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.saveAndFlush(any(UserAccount.class))).thenReturn(savedUser);

        authService.register(request);

        verify(userRepository).existsByEmail("john@example.com");
    }

    @Test
    void register_shouldThrowConflictExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("john@example.com", "password123", "John Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_shouldThrowConflictExceptionOnDataIntegrityViolation() {
        RegisterRequest request = new RegisterRequest("john@example.com", "password123", "John Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.saveAndFlush(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    void register_shouldTrimDisplayName() {
        RegisterRequest request = new RegisterRequest("john@example.com", "password123", "  John Doe  ");
        UserAccount savedUser = new UserAccount("john@example.com", "encoded_password", "John Doe");
        savedUser.setId(1L);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.saveAndFlush(any(UserAccount.class))).thenReturn(savedUser);

        authService.register(request);

        verify(userRepository).saveAndFlush(any(UserAccount.class));
    }

    @Test
    void login_shouldAuthenticateValidCredentials() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        UserAccount user = new UserAccount("john@example.com", "encoded_password", "John Doe");
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.from(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.issue(principal)).thenReturn("jwt_token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt_token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void login_shouldNormalizeEmail() {
        LoginRequest request = new LoginRequest("JOHN@EXAMPLE.COM", "password123");
        UserAccount user = new UserAccount("john@example.com", "encoded_password", "John Doe");
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.from(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.issue(principal)).thenReturn("jwt_token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowInvalidCredentialsExceptionOnBadCredentials() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldThrowInvalidCredentialsExceptionOnAuthenticationException() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.AuthenticationException("Auth failed") {
                });

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
