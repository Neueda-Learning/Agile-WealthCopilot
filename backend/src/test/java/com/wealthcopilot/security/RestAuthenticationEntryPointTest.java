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
import org.springframework.security.core.AuthenticationException;

import com.wealthcopilot.exception.ApiErrorWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RestAuthenticationEntryPointTest {

    @Mock
    private ApiErrorWriter errorWriter;

    private RestAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        entryPoint = new RestAuthenticationEntryPoint(errorWriter);
    }

    @Test
    void commence_shouldCallErrorWriterWithUnauthorizedStatus() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);

        entryPoint.commence(request, response, authException);

        verify(errorWriter).write(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required");
    }

    @Test
    void commence_shouldWriteCorrectErrorCode() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);

        entryPoint.commence(request, response, authException);

        verify(errorWriter).write(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyInt(),
                anyString(),
                anyString());
    }

    @Test
    void commence_shouldHandleMultipleRequests() throws IOException, ServletException {
        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletResponse response1 = mock(HttpServletResponse.class);
        AuthenticationException authException1 = mock(AuthenticationException.class);

        HttpServletRequest request2 = mock(HttpServletRequest.class);
        HttpServletResponse response2 = mock(HttpServletResponse.class);
        AuthenticationException authException2 = mock(AuthenticationException.class);

        entryPoint.commence(request1, response1, authException1);
        entryPoint.commence(request2, response2, authException2);

        verify(errorWriter).write(request1, response1, 401, "UNAUTHORIZED", "Authentication is required");
        verify(errorWriter).write(request2, response2, 401, "UNAUTHORIZED", "Authentication is required");
    }

    @Test
    void commence_shouldPassCorrectErrorMessage() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);

        entryPoint.commence(request, response, authException);

        verify(errorWriter).write(
                any(),
                any(),
                anyInt(),
                anyString(),
                org.mockito.ArgumentMatchers.contains("Authentication is required"));
    }
}
