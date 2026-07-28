package com.wealthcopilot.dto.auth;

import com.wealthcopilot.entity.UserAccount;

public record UserResponse(Long id, String email, String displayName) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
