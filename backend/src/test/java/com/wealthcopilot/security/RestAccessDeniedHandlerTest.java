package com.wealthcopilot.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.wealthcopilot.exception.ApiErrorWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RestAccessDeniedHandlerTest {

    @Mock
    private ApiErrorWriter errorWriter;

    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        accessDeniedHandler = new RestAccessDeniedHandler(errorWriter);
    }

    @Test
    void handle_shouldCallErrorWriterWithForbiddenStatus() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        accessDeniedHandler.handle(request, response, accessDeniedException);

        verify(errorWriter).write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN",
                "Access is denied");
    }

    @Test
    void handle_shouldWriteCorrectStatusCode() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        accessDeniedHandler.handle(request, response, accessDeniedException);

        verify(errorWriter).write(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                org.mockito.ArgumentMatchers.eq(403),
                anyString(),
                anyString());
    }

    @Test
    void handle_shouldHandleMultipleRequests() throws IOException, ServletException {
        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletResponse response1 = mock(HttpServletResponse.class);
        AccessDeniedException exception1 = mock(AccessDeniedException.class);

        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        AccessDeniedException exception2 = mock(AccessDeniedException.class);

        accessDeniedHandler.handle(request1, response1, exception1);
        accessDeniedHandler.handle(request2, response2, exception2);

        verify(errorWriter).write(request1, response1, 403, "FORBIDDEN", "Access is denied");
        verify(errorWriter).write(request2, response2, 403, "FORBIDDEN", "Access is denied");
    }

    @Test
    void handle_shouldPassCorrectErrorMessage() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        accessDeniedHandler.handle(request, response, accessDeniedException);

        verify(errorWriter).write(
                any(),
                any(),
                anyInt(),
                anyString(),
                org.mockito.ArgumentMatchers.contains("Access is denied"));
    }

    @Test
    void handle_shouldPassForbiddenErrorCode() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        accessDeniedHandler.handle(request, response, accessDeniedException);

        verify(errorWriter).write(
                any(),
                any(),
                anyInt(),
                org.mockito.ArgumentMatchers.contains("FORBIDDEN"),
                any());
    }
}
