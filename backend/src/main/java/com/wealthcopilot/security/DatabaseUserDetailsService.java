package com.wealthcopilot.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.wealthcopilot.repository.UserAccountRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepository;

    public DatabaseUserDetailsService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
