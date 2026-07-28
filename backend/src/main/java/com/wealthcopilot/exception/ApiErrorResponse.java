package com.wealthcopilot.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<ApiErrorDetail> details,
        Instant timestamp,
        String path) {
}
