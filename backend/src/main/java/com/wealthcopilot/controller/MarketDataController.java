package com.wealthcopilot.controller;

import com.wealthcopilot.dto.response.MarketQuoteResponse;
import com.wealthcopilot.dto.response.SymbolSearchResponse;
import com.wealthcopilot.service.MarketDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/quote/{ticker}")
    public MarketQuoteResponse quote(@PathVariable String ticker) {
        return marketDataService.getQuote(ticker);
    }

    @GetMapping("/search")
    public List<SymbolSearchResponse> search(@RequestParam String query) {
        return marketDataService.search(query);
    }
}
