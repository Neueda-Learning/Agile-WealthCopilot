package com.wealthcopilot.marketdata;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketDataConfiguration {

    @Bean
    public Cache<String, List<MarketDataClient.SymbolSearchResult>> symbolSearchCache(
            MarketDataProperties properties
    ) {
        return Caffeine.newBuilder()
                .expireAfterWrite(properties.getSearchCacheTtl())
                .maximumSize(properties.getSearchCacheSize())
                .build();
    }
}
