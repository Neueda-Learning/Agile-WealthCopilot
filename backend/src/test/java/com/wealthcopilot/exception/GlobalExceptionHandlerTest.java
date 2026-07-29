package com.wealthcopilot.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
    }

    @Test
    void handleValidation_shouldReturnBadRequest() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleValidation_shouldReturnApiErrorResponse() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleValidation(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void handleUnreadableBody_shouldReturnBadRequest() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleUnreadableBody(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleUnreadableBody_shouldReturnValidationFailedCode() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleUnreadableBody(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void handleConflict_shouldReturnConflictStatus() {
        ConflictException exception = new ConflictException("Email already exists");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleConflict(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleConflict_shouldIncludeExceptionMessage() {
        ConflictException exception = new ConflictException("Email already exists");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleConflict(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Email already exists");
    }

    @Test
    void handleInvalidCredentials_shouldReturnUnauthorizedStatus() {
        InvalidCredentialsException exception = new InvalidCredentialsException();

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleInvalidCredentials(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleInvalidCredentials_shouldReturnUnauthorizedCode() {
        InvalidCredentialsException exception = new InvalidCredentialsException();

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleInvalidCredentials(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void handleNotFound_shouldReturnNotFoundStatus() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleNotFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleNotFound_shouldReturnNotFoundCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleNotFound(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleMarketDataUnavailable_shouldReturnServiceUnavailableStatus() {
        MarketDataUnavailableException exception = new MarketDataUnavailableException("Market data unavailable");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleMarketDataUnavailable(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handleMarketDataUnavailable_shouldReturnCorrectCode() {
        MarketDataUnavailableException exception = new MarketDataUnavailableException("Market data unavailable");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleMarketDataUnavailable(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MARKET_DATA_UNAVAILABLE");
    }

    @Test
    void handleDomainValidation_shouldReturnBadRequestStatus() {
        DomainValidationException exception = new DomainValidationException("Invalid domain");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleDomainValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleDomainValidation_shouldReturnValidationFailedCode() {
        DomainValidationException exception = new DomainValidationException("Invalid domain");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler
                .handleDomainValidation(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void allExceptionHandlers_shouldReturnApiErrorResponse() {
        ConflictException conflictEx = new ConflictException("Conflict");
        InvalidCredentialsException invalidCredsEx = new InvalidCredentialsException();
        ResourceNotFoundException notFoundEx = new ResourceNotFoundException("Not found");

        ResponseEntity<ApiErrorResponse> response1 = globalExceptionHandler
                .handleConflict(conflictEx, request);
        ResponseEntity<ApiErrorResponse> response2 = globalExceptionHandler
                .handleInvalidCredentials(invalidCredsEx, request);
        ResponseEntity<ApiErrorResponse> response3 = globalExceptionHandler
                .handleNotFound(notFoundEx, request);

        assertThat(response1.getBody()).isNotNull();
        assertThat(response2.getBody()).isNotNull();
        assertThat(response3.getBody()).isNotNull();
    }
}
