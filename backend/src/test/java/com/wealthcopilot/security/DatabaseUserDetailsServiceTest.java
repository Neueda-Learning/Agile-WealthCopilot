package com.wealthcopilot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.wealthcopilot.entity.UserAccount;
import com.wealthcopilot.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    private DatabaseUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new DatabaseUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {
        String email = "john@example.com";
        UserAccount user = new UserAccount(email, "encoded_password", "John Doe");
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(UserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_shouldNormalizeEmail() {
        String email = "  JOHN@EXAMPLE.COM  ";
        String normalizedEmail = "john@example.com";
        UserAccount user = new UserAccount(normalizedEmail, "encoded_password", "John Doe");
        user.setId(1L);

        when(userRepository.findByEmail(normalizedEmail)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        verify(userRepository).findByEmail(normalizedEmail);
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loadUserByUsername_shouldHandleNullUsername() {
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    void loadUserByUsername_shouldPreserveUserProperties() {
        String email = "john@example.com";
        String displayName = "John Doe";
        String password = "encoded_password";
        UserAccount user = new UserAccount(email, password, displayName);
        user.setId(42L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        UserPrincipal principal = (UserPrincipal) userDetails;
        assertThat(principal.getUsername()).isEqualTo(email);
        assertThat(principal.getPassword()).isEqualTo(password);
        assertThat(principal.userId()).isEqualTo(42L);
    }

    @Test
    void loadUserByUsername_shouldReturnEnabledAccount() {
        String email = "john@example.com";
        UserAccount user = new UserAccount(email, "encoded_password", "John Doe");
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }
}
