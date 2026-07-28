package com.wealthcopilot.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        UserAccount user = new UserAccount(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim());
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email is already registered");
        }
    }

    public LoginResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizeEmail(request.email()),
                            request.password()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return new LoginResponse(
                    jwtService.issue(principal),
                    "Bearer",
                    jwtService.expirationSeconds());
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException();
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return userRepository.findByIdAndAuthenticatedUserId(userId, userId)
                .map(UserResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> findUserOwnedBy(Long userId, Long resourceId) {
        return userRepository
                .findByIdAndAuthenticatedUserId(resourceId, userId)
                .map(UserResponse::from);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
