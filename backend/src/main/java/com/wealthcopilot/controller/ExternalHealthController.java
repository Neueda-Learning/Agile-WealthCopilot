package com.wealthcopilot.controller;

import com.wealthcopilot.dto.response.ExternalHealthResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external")
public class ExternalHealthController {

    private final Clock clock;
    private final String version;

    public ExternalHealthController(
            Clock clock,
            @Value("${spring.application.version:1.0.0}") String version
    ) {
        this.clock = clock;
        this.version = version;
    }

    @GetMapping("/health")
    public ExternalHealthResponse health() {
        return new ExternalHealthResponse("UP", version, Instant.now(clock));
    }
}
