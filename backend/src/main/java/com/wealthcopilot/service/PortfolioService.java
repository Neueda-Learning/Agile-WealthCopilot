package com.wealthcopilot.service;

import com.wealthcopilot.dto.response.HoldingResponse;
import com.wealthcopilot.dto.response.PerformanceSummaryResponse;
import com.wealthcopilot.dto.response.PortfolioSummaryResponse;
import com.wealthcopilot.dto.response.TransactionResponse;
import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import com.wealthcopilot.exception.DomainValidationException;
import com.wealthcopilot.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService implements EzioAgentToolService {

    private static final int MONEY_SCALE = 4;
    private static final int PCT_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final PriceQuoteProvider priceQuoteProvider;

    public PortfolioService(
            TransactionRepository transactionRepository,
            TransactionService transactionService,
            PriceQuoteProvider priceQuoteProvider
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.priceQuoteProvider = priceQuoteProvider;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "portfolioSummary", key = "#userId")
    public PortfolioSummaryResponse getPortfolioSummary(Long userId) {
        List<Transaction> timeline = transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId);
        PortfolioMath.Result result = PortfolioMath.replay(timeline);

        List<HoldingResponse> holdings = buildHoldings(result.positions());

        BigDecimal totalCostBasis = holdings.stream()
                .map(HoldingResponse::costBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = holdings.stream()
                .map(HoldingResponse::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unrealized = totalValue.subtract(totalCostBasis);
        BigDecimal unrealizedPct = totalCostBasis.signum() == 0
                ? BigDecimal.ZERO
                : unrealized.multiply(BigDecimal.valueOf(100))
                        .divide(totalCostBasis, PCT_SCALE, RoundingMode.HALF_UP);

        BigDecimal dayChange = BigDecimal.ZERO;
        BigDecimal dayBase = BigDecimal.ZERO;
        boolean stale = false;
        LocalDateTime latestPriceTime = null;

        for (HoldingResponse holding : holdings) {
            if (holding.currentPrice() != null && holding.dayChangePct() != null) {
                BigDecimal previous = holding.currentPrice()
                        .divide(
                                BigDecimal.ONE
                                        .add(holding.dayChangePct().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)),
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );
                BigDecimal base = previous.multiply(holding.quantity());
                BigDecimal change = holding.marketValue().subtract(base);
                dayBase = dayBase.add(base);
                dayChange = dayChange.add(change);
            }

            if (holding.stale()) {
                stale = true;
            }
            if (holding.priceAsOf() != null && (latestPriceTime == null || holding.priceAsOf().isAfter(latestPriceTime))) {
                latestPriceTime = holding.priceAsOf();
            }
        }

        BigDecimal dayChangePct = dayBase.signum() == 0
                ? null
                : dayChange.multiply(BigDecimal.valueOf(100)).divide(dayBase, PCT_SCALE, RoundingMode.HALF_UP);

        return new PortfolioSummaryResponse(
                scaleMoney(totalValue),
                scaleMoney(totalCostBasis),
                scaleMoney(unrealized),
                unrealizedPct,
                scaleMoney(result.realizedPnl()),
                scaleMoney(dayChange),
                dayChangePct,
                "USD",
                latestPriceTime,
                stale
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "portfolioHoldings", key = "#userId")
    public List<HoldingResponse> getHoldings(Long userId) {
        List<Transaction> timeline = transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId);
        PortfolioMath.Result result = PortfolioMath.replay(timeline);
        return buildHoldings(result.positions());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId, String ticker, LocalDate from, LocalDate to) {
        return transactionService.listTransactions(userId, ticker, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceSummaryResponse getInvestedAmount(Long userId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new DomainValidationException("from must be less than or equal to to");
        }

        List<Transaction> timeline = transactionRepository.findAllByUserIdOrderByTradeDateAscIdAsc(userId);

        BigDecimal investedAmount = BigDecimal.ZERO;
        BigDecimal proceedsAmount = BigDecimal.ZERO;
        BigDecimal realizedInRange = BigDecimal.ZERO;
        long buyCount = 0;
        long sellCount = 0;

        Map<String, PortfolioMath.PositionState> states = new java.util.LinkedHashMap<>();

        for (Transaction tx : timeline) {
            String ticker = tx.getInstrument().getTicker().toUpperCase();
            PortfolioMath.PositionState state = states.computeIfAbsent(ticker, t -> new PortfolioMath.PositionState());

            BigDecimal qty = tx.getQuantity();
            BigDecimal notional = tx.getPrice().multiply(qty);
            BigDecimal fees = tx.getFees() == null ? BigDecimal.ZERO : tx.getFees();
            boolean inRange = isInRange(tx.getTradeDate(), from, to);

            if (tx.getSide() == TransactionSide.BUY) {
                if (inRange) {
                    investedAmount = investedAmount.add(notional).add(fees);
                    buyCount++;
                }
                state.applyBuy(qty, notional, fees);
            } else {
                BigDecimal avgCost = state.getTotalQuantity().signum() == 0
                        ? BigDecimal.ZERO
                        : state.getTotalCost().divide(state.getTotalQuantity(), MONEY_SCALE, RoundingMode.HALF_UP);
                BigDecimal costRemoved = avgCost.multiply(qty);
                BigDecimal proceeds = notional.subtract(fees);
                BigDecimal realized = proceeds.subtract(costRemoved);

                if (inRange) {
                    proceedsAmount = proceedsAmount.add(proceeds);
                    realizedInRange = realizedInRange.add(realized);
                    sellCount++;
                }

                state.applySell(qty, costRemoved);
            }
        }

        return new PerformanceSummaryResponse(
                from,
                to,
                scaleMoney(investedAmount),
                scaleMoney(proceedsAmount),
                scaleMoney(investedAmount.subtract(proceedsAmount)),
                scaleMoney(realizedInRange),
                buyCount,
                sellCount,
                "USD"
        );
    }

    private List<HoldingResponse> buildHoldings(Map<String, PortfolioMath.PositionState> positions) {
        List<HoldingResponse> holdings = new ArrayList<>();

        for (Map.Entry<String, PortfolioMath.PositionState> entry : positions.entrySet()) {
            String ticker = entry.getKey();
            PortfolioMath.PositionState state = entry.getValue();
            if (state.getTotalQuantity().signum() == 0) {
                continue;
            }

            BigDecimal qty = state.getTotalQuantity();
            BigDecimal costBasis = state.getTotalCost();
            BigDecimal avgCost = state.avgCost();

            PriceQuoteProvider.QuoteSnapshot quote = priceQuoteProvider.getLatestQuote(ticker).orElse(null);
            BigDecimal currentPrice = quote == null ? null : quote.price();
            BigDecimal marketValue = currentPrice == null ? BigDecimal.ZERO : currentPrice.multiply(qty);
            BigDecimal unrealized = marketValue.subtract(costBasis);
            BigDecimal unrealizedPct = costBasis.signum() == 0
                    ? BigDecimal.ZERO
                    : unrealized.multiply(BigDecimal.valueOf(100)).divide(costBasis, PCT_SCALE, RoundingMode.HALF_UP);

            BigDecimal dayChangePct = null;
            if (quote != null && quote.previousClose() != null && quote.previousClose().signum() > 0) {
                dayChangePct = quote.price().subtract(quote.previousClose())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(quote.previousClose(), PCT_SCALE, RoundingMode.HALF_UP);
            }

            holdings.add(new HoldingResponse(
                    ticker,
                    ticker,
                    "STOCK",
                    qty,
                    scaleMoney(avgCost),
                    scaleMoney(costBasis),
                    currentPrice == null ? null : scaleMoney(currentPrice),
                    scaleMoney(marketValue),
                    scaleMoney(unrealized),
                    unrealizedPct,
                    dayChangePct,
                    BigDecimal.ZERO,
                    quote == null ? null : quote.asOf(),
                    quote != null && quote.stale()
            ));
        }

        BigDecimal totalValue = holdings.stream().map(HoldingResponse::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<HoldingResponse> withWeights = new ArrayList<>(holdings.size());

        for (HoldingResponse holding : holdings) {
            BigDecimal weight = totalValue.signum() == 0
                    ? BigDecimal.ZERO
                    : holding.marketValue().multiply(BigDecimal.valueOf(100)).divide(totalValue, PCT_SCALE, RoundingMode.HALF_UP);
            withWeights.add(new HoldingResponse(
                    holding.ticker(),
                    holding.name(),
                    holding.type(),
                    holding.quantity(),
                    holding.avgCost(),
                    holding.costBasis(),
                    holding.currentPrice(),
                    holding.marketValue(),
                    holding.unrealizedPnl(),
                    holding.unrealizedPnlPct(),
                    holding.dayChangePct(),
                    weight,
                    holding.priceAsOf(),
                    holding.stale()
            ));
        }

        withWeights.sort(Comparator.comparing(HoldingResponse::ticker));
        return withWeights;
    }

    private boolean isInRange(LocalDate value, LocalDate from, LocalDate to) {
        if (from != null && value.isBefore(from)) {
            return false;
        }
        if (to != null && value.isAfter(to)) {
            return false;
        }
        return true;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
