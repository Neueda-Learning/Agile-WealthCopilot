package com.wealthcopilot.service;

import java.util.Optional;

public class NoOpPriceQuoteProvider implements PriceQuoteProvider {

    @Override
    public Optional<QuoteSnapshot> getLatestQuote(String ticker) {
        return Optional.empty();
    }
}
