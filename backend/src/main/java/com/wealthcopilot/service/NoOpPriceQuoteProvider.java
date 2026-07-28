package com.wealthcopilot.service;

import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NoOpPriceQuoteProvider implements PriceQuoteProvider {

    @Override
    public Optional<QuoteSnapshot> getLatestQuote(String ticker) {
        return Optional.empty();
    }
}
