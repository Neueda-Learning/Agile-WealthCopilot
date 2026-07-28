package com.wealthcopilot.controller;

import com.wealthcopilot.dto.response.HoldingResponse;
import com.wealthcopilot.dto.response.PerformanceSummaryResponse;
import com.wealthcopilot.dto.response.PortfolioSummaryResponse;
import com.wealthcopilot.service.PortfolioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(@RequestAttribute("userId") Long userId) {
        return portfolioService.getPortfolioSummary(userId);
    }

    @GetMapping("/holdings")
    public List<HoldingResponse> holdings(@RequestAttribute("userId") Long userId) {
        return portfolioService.getHoldings(userId);
    }

    @GetMapping("/performance")
    public PerformanceSummaryResponse performance(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return portfolioService.getInvestedAmount(userId, from, to);
    }
}
