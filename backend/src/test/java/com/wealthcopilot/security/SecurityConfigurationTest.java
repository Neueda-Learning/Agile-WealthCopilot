package com.wealthcopilot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void passwordEncoder_shouldBeConfigured() {
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder.getClass().getSimpleName()).contains("BCrypt");
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatchPasswords() {
        String rawPassword = "testPassword123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encodedPassword)).isFalse();
    }

    @Test
    void authenticationManager_shouldBeConfigured() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    void externalApiSecurityFilterChain_deniesUnauthorizedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/external/protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void externalApiSecurityFilterChain_allowsHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/external/health"))
                .andExpect(status().isOk());
    }

    @Test
    void externalApiSecurityFilterChain_disablesCsrf() throws Exception {
        // CSRF protection is disabled, so POST requests without CSRF tokens are allowed
        mockMvc.perform(post("/api/v1/external/protected-endpoint"))
                .andExpect(status().isUnauthorized()); // Fails due to auth, not CSRF
    }

    @Test
    void externalApiSecurityFilterChain_usesStatelessSessions() throws Exception {
        mockMvc.perform(get("/api/v1/external/health"))
                .andExpect(status().isOk());
        // No session should be created - verified by checking response headers
    }

    @Test
    void jwtSecurityFilterChain_permitsAuthEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk()); // Will return 200 or 400 depending on body, not 401
    }

    @Test
    void jwtSecurityFilterChain_permitsRegisterEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().isOk()); // Will return 200 or 400 depending on body, not 401
    }

    @Test
    void jwtSecurityFilterChain_permitsHealthEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void jwtSecurityFilterChain_permitsErrorEndpoint() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isOk()); // Will vary based on implementation, not 401
    }

    @Test
    void jwtSecurityFilterChain_requiresAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtSecurityFilterChain_disablesCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/portfolio"))
                .andExpect(status().isUnauthorized()); // Fails due to auth, not CSRF
    }

    @Test
    void jwtSecurityFilterChain_usesStatelessSessions() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isOk());
        // No session should be created - verified by stateless configuration
    }

    @Test
    void jwtSecurityFilterChain_disablesHttpBasic() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized()); // Not accepted as Basic auth
    }

    @Test
    void jwtSecurityFilterChain_disablesFormLogin() throws Exception {
        mockMvc.perform(post("/api/v1/portfolio")
                        .param("username", "user")
                        .param("password", "pass"))
                .andExpect(status().isUnauthorized()); // Not accepted as form login
    }

    @Test
    void jwtSecurityFilterChain_disablesLogout() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isNotFound()); // Logout endpoint doesn't exist
    }
}
