package com.wealthcopilot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wealthcopilot.dto.response.MarketQuoteResponse;
import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.entity.InstrumentType;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.exception.ResourceNotFoundException;
import com.wealthcopilot.service.MarketDataService;
import java.math.BigDecimal;
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
class MarketDataControllerTest {

    @Mock
    private MarketDataService marketDataService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MarketDataController(marketDataService)).build();
    }

    @Test
    void quote_returnsDatabaseBackedQuote() throws Exception {
        when(marketDataService.getQuote("NVDA")).thenReturn(new MarketQuoteResponse(
                "NVDA",
                new BigDecimal("181.1000"),
                new BigDecimal("179.0000"),
                LocalDateTime.parse("2026-07-28T12:00:00"),
                false
        ));

        mockMvc.perform(get("/api/v1/market/quote/NVDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("NVDA"))
                .andExpect(jsonPath("$.price").value(181.1))
                .andExpect(jsonPath("$.stale").value(false));
    }

    @Test
    void quote_returnsNotFoundForUnknownTicker() throws Exception {
        when(marketDataService.getQuote("NOPE"))
                .thenThrow(new ResourceNotFoundException("unknown ticker: NOPE"));

        mockMvc.perform(get("/api/v1/market/quote/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsSupportedInstruments() throws Exception {
        when(marketDataService.search("nvidia")).thenReturn(List.of(
                new SymbolSearchResponse(
                        "NVDA",
                        "NVIDIA Corporation",
                        "NASDAQ",
                        InstrumentType.STOCK,
                        "USD"
                )
        ));

        mockMvc.perform(get("/api/v1/market/search").param("query", "nvidia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("NVDA"))
                .andExpect(jsonPath("$[0].type").value("STOCK"))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    void search_rejectsBlankQuery() throws Exception {
        when(marketDataService.search("")).thenThrow(new DomainValidationException("query is required"));

        mockMvc.perform(get("/api/v1/market/search").param("query", ""))
                .andExpect(status().isBadRequest());
    }
}
