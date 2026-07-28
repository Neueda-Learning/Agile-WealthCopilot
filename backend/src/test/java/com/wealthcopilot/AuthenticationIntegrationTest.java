package com.wealthcopilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthcopilot.dto.auth.UserResponse;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.repository.UserAccountRepository;
import com.wealthcopilot.security.UserPrincipal;
import com.wealthcopilot.service.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Import(AuthenticationIntegrationTest.OwnershipProbeController.class)
class AuthenticationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("wealthcopilot")
            .withUsername("wealthcopilot")
            .withPassword("wealthcopilot_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerHashesPasswordAndReturnsDocumentedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "SIMON@example.com",
                                  "password": "correct-horse",
                                  "displayName": " Simon "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("simon@example.com"))
                .andExpect(jsonPath("$.displayName").value("Simon"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var storedUser = userRepository.findByEmail("simon@example.com").orElseThrow();
        assertThat(storedUser.getPasswordHash()).startsWith("$2");
        assertThat(storedUser.getPasswordHash()).isNotEqualTo("correct-horse");
        assertThat(passwordEncoder.matches("correct-horse", storedUser.getPasswordHash())).isTrue();
    }

    @Test
    void loginIssuesJwtAndMeUsesAuthenticatedPrincipal() throws Exception {
        JsonNode registered = register("simon@example.com", "correct-horse", "Simon");
        String token = login("SIMON@example.com", "correct-horse");
        var decodedToken = jwtDecoder.decode(token);

        assertThat(decodedToken.getHeaders().get("alg")).isEqualTo("HS256");
        assertThat(decodedToken.getClaimAsString("iss")).isEqualTo("wealthcopilot");
        assertThat(decodedToken.getSubject()).isEqualTo(registered.get("id").asText());
        assertThat(Duration.between(
                decodedToken.getIssuedAt(),
                decodedToken.getExpiresAt())).isEqualTo(Duration.ofHours(1));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registered.get("id").longValue()))
                .andExpect(jsonPath("$.email").value("simon@example.com"))
                .andExpect(jsonPath("$.displayName").value("Simon"));
    }

    @Test
    void invalidCredentialsAndMissingTokenReturnUniform401() throws Exception {
        register("simon@example.com", "correct-horse", "Simon");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "simon@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validationAndDuplicateEmailReturnDocumentedErrors() throws Exception {
        register("simon@example.com", "correct-horse", "Simon");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "SIMON@example.com",
                                  "password": "another-password",
                                  "displayName": "Other Simon"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "displayName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void crossUserAccessIsScopedInRepositoryAndReturns404Not403() throws Exception {
        JsonNode userA = register("user-a@example.com", "password-a", "User A");
        JsonNode userB = register("user-b@example.com", "password-b", "User B");
        String userBToken = login("user-b@example.com", "password-b");

        assertThat(userRepository.findByIdAndAuthenticatedUserId(
                userA.get("id").longValue(),
                userB.get("id").longValue())).isEmpty();

        mockMvc.perform(get("/api/v1/test/users/{id}", userA.get("id").longValue())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(
                        "/api/v1/test/users/" + userA.get("id").longValue()));

        mockMvc.perform(get("/api/v1/test/users/{id}", userB.get("id").longValue())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userB.get("id").longValue()));
    }

    @Test
    void globalErrorHandlerReturnsUniform404And500Bodies() throws Exception {
        register("simon@example.com", "correct-horse", "Simon");
        String token = login("simon@example.com", "correct-horse");

        mockMvc.perform(get("/api/v1/does-not-exist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/does-not-exist"));

        mockMvc.perform(get("/api/v1/test/users/boom")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/test/users/boom"));
    }

    private JsonNode register(String email, String password, String displayName) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Registration(
                                email,
                                password,
                                displayName))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Login(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").textValue();
    }

    private record Registration(String email, String password, String displayName) {
    }

    private record Login(String email, String password) {
    }

    @RestController
    @RequestMapping("/api/v1/test/users")
    static class OwnershipProbeController {

        private final AuthService authService;

        OwnershipProbeController(AuthService authService) {
            this.authService = authService;
        }

        @GetMapping("/{id}")
        UserResponse getUser(
                @AuthenticationPrincipal UserPrincipal principal,
                @PathVariable Long id) {
            return authService.findUserOwnedBy(principal.userId(), id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        @GetMapping("/boom")
        UserResponse fail() {
            throw new IllegalStateException("Test-only failure");
        }
    }
}
