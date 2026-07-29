package com.wealthcopilot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wealthcopilot.dto.response.PriceRefreshResponse;
import com.wealthcopilot.service.HoldingPriceRefreshService;
import com.wealthcopilot.service.PortfolioService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private HoldingPriceRefreshService priceRefreshService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PortfolioController(portfolioService, priceRefreshService))
                .build();
    }

    @Test
    void refreshHoldings_waitsForAndReturnsTheRefreshResult() throws Exception {
        when(priceRefreshService.refreshHeldPrices(7L)).thenReturn(new PriceRefreshResponse(
                2,
                2,
                List.of(),
                LocalDateTime.parse("2026-07-29T12:00:00")));

        mockMvc.perform(post("/api/v1/portfolio/holdings/refresh")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.refreshed").value(2))
                .andExpect(jsonPath("$.failedTickers").isEmpty());
    }
}
