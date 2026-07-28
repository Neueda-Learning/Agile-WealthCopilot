package com.wealthcopilot.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.wealthcopilot.entity.UserAccount;

public final class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;

    private UserPrincipal(Long userId, String email, String passwordHash) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public static UserPrincipal from(UserAccount user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    public Long userId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
