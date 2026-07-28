package com.wealthcopilot.dto.response;

import java.time.Instant;

public record ExternalHealthResponse(
        String status,
        String version,
        Instant time
) {
}
