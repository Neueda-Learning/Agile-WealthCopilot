package com.wealthcopilot.controller;

import com.wealthcopilot.dto.response.HoldingResponse;
import com.wealthcopilot.dto.response.PerformanceSummaryResponse;
import com.wealthcopilot.dto.response.PortfolioSummaryResponse;
import com.wealthcopilot.dto.response.PriceRefreshResponse;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.service.HoldingPriceRefreshService;
import com.wealthcopilot.service.PortfolioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final HoldingPriceRefreshService priceRefreshService;

    public PortfolioController(
            PortfolioService portfolioService,
            HoldingPriceRefreshService priceRefreshService
    ) {
        this.portfolioService = portfolioService;
        this.priceRefreshService = priceRefreshService;
    }

    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(@RequestAttribute("userId") Long userId) {
        return portfolioService.getPortfolioSummary(userId);
    }

    @GetMapping("/holdings")
    public List<HoldingResponse> holdings(@RequestAttribute("userId") Long userId) {
        return portfolioService.getHoldings(userId);
    }

    @PostMapping("/holdings/refresh")
    public PriceRefreshResponse refreshHoldings(@RequestAttribute("userId") Long userId) {
        return priceRefreshService.refreshHeldPrices(userId);
    }

    @GetMapping("/performance")
    public PerformanceSummaryResponse performance(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null && to == null && range != null && !range.isBlank() && !"ALL".equalsIgnoreCase(range)) {
            to = LocalDate.now();
            from = switch (range.toUpperCase()) {
                case "1M" -> to.minusMonths(1);
                case "3M" -> to.minusMonths(3);
                case "6M" -> to.minusMonths(6);
                case "1Y" -> to.minusYears(1);
                default -> throw new DomainValidationException("range must be one of 1M, 3M, 6M, 1Y, or ALL");
            };
        }
        return portfolioService.getInvestedAmount(userId, from, to);
    }
}
