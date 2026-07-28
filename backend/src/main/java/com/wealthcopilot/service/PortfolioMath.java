package com.wealthcopilot.service;

import com.wealthcopilot.entity.Transaction;
import com.wealthcopilot.entity.TransactionSide;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PortfolioMath {

    private static final int MONEY_SCALE = 4;
    private static final int QTY_SCALE = 6;

    private PortfolioMath() {
    }

    public static Result replay(List<Transaction> transactions) {
        Map<String, PositionState> states = new LinkedHashMap<>();
        BigDecimal realizedPnl = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            String ticker = tx.getInstrument().getTicker().toUpperCase();
            PositionState state = states.computeIfAbsent(ticker, t -> new PositionState());

            BigDecimal quantity = tx.getQuantity().setScale(QTY_SCALE, RoundingMode.HALF_UP);
            BigDecimal notional = tx.getPrice().multiply(quantity);
            BigDecimal fees = tx.getFees() == null ? BigDecimal.ZERO : tx.getFees();

            if (tx.getSide() == TransactionSide.BUY) {
                state.applyBuy(quantity, notional, fees);
            } else {
                BigDecimal averageCost = state.totalQuantity.signum() == 0
                        ? BigDecimal.ZERO
                        : state.totalCost.divide(state.totalQuantity, MONEY_SCALE, RoundingMode.HALF_UP);
                BigDecimal costRemoved = averageCost.multiply(quantity);
                BigDecimal proceeds = notional.subtract(fees);
                realizedPnl = realizedPnl.add(proceeds.subtract(costRemoved));
                state.applySell(quantity, costRemoved);
            }
        }

        return new Result(states, realizedPnl.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    public static final class PositionState {
        private BigDecimal totalQuantity = BigDecimal.ZERO.setScale(QTY_SCALE, RoundingMode.HALF_UP);
        private BigDecimal totalCost = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        public void applyBuy(BigDecimal quantity, BigDecimal notional, BigDecimal fees) {
            totalQuantity = totalQuantity.add(quantity);
            totalCost = totalCost.add(notional).add(fees);
        }

        public void applySell(BigDecimal quantity, BigDecimal costRemoved) {
            totalQuantity = totalQuantity.subtract(quantity);
            totalCost = totalCost.subtract(costRemoved);
            if (totalQuantity.signum() == 0) {
                totalCost = BigDecimal.ZERO;
            }
        }

        public BigDecimal getTotalQuantity() {
            return totalQuantity;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public BigDecimal avgCost() {
            if (totalQuantity.signum() == 0) {
                return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            return totalCost.divide(totalQuantity, MONEY_SCALE, RoundingMode.HALF_UP);
        }
    }

    public record Result(
            Map<String, PositionState> positions,
            BigDecimal realizedPnl
    ) {
    }
}
