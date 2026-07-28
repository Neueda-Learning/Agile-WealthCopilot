package com.wealthcopilot.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_rejectsInvalidKey() throws Exception {
        MockHttpServletRequest request = request("/api/v1/external/stats");
        request.addHeader("X-API-Key", "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(apiKeyService.authenticate("invalid")).thenReturn(false);

        new ApiKeyAuthenticationFilter(apiKeyService)
                .doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        verify(apiKeyService).authenticate("invalid");
    }

    @Test
    void filter_authenticatesValidReadOnlyKey() throws Exception {
        MockHttpServletRequest request = request("/api/v1/external/stats");
        request.addHeader("X-API-Key", "valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(apiKeyService.authenticate("valid")).thenReturn(true);

        new ApiKeyAuthenticationFilter(apiKeyService)
                .doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                "SCOPE_READ_ONLY",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority()
        );
    }

    @Test
    void filter_skipsPublicHealthEndpoint() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ApiKeyAuthenticationFilter(apiKeyService)
                .doFilter(request("/api/v1/external/health"), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void filter_skipsNonExternalEndpoint() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ApiKeyAuthenticationFilter(apiKeyService)
                .doFilter(request("/api/v1/market/search"), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }
}
