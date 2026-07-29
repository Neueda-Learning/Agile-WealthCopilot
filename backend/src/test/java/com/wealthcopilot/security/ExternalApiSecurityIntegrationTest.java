package com.wealthcopilot.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false",
        "external-api.bootstrap-key=integration-test-key"
})
@AutoConfigureMockMvc
class ExternalApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/external/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void marketEndpoints_requireJwtRatherThanAnExternalApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/market/quote/NOPE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedExternalEndpoint_rejectsMissingKey() throws Exception {
        mockMvc.perform(get("/api/v1/external/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void externalSecurity_allowsValidKeyToReachReadOnlyRouting() throws Exception {
        mockMvc.perform(get("/api/v1/external/stats")
                        .header("X-API-Key", "integration-test-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void externalSecurity_deniesWritesEvenWithValidKey() throws Exception {
        mockMvc.perform(post("/api/v1/external/stats")
                        .header("X-API-Key", "integration-test-key"))
                .andExpect(status().isForbidden());
    }
}
