package com.wealthcopilot.marketdata;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {

    private String baseUrl = "https://api.twelvedata.com";
    private String apiKey = "";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Duration cacheTtl = Duration.ofMinutes(15);
    private Duration searchCacheTtl = Duration.ofMinutes(10);
    private long searchCacheSize = 500;
    private int creditsPerMinute = 8;
    private int maxAttempts = 3;
    private Duration retryInitialBackoff = Duration.ofMillis(250);
    private Duration maxRetryAfter = Duration.ofSeconds(5);
    private ZoneId marketZone = ZoneId.of("America/New_York");

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public Duration getSearchCacheTtl() {
        return searchCacheTtl;
    }

    public void setSearchCacheTtl(Duration searchCacheTtl) {
        this.searchCacheTtl = searchCacheTtl;
    }

    public long getSearchCacheSize() {
        return searchCacheSize;
    }

    public void setSearchCacheSize(long searchCacheSize) {
        this.searchCacheSize = searchCacheSize;
    }

    public int getCreditsPerMinute() {
        return creditsPerMinute;
    }

    public void setCreditsPerMinute(int creditsPerMinute) {
        this.creditsPerMinute = creditsPerMinute;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getRetryInitialBackoff() {
        return retryInitialBackoff;
    }

    public void setRetryInitialBackoff(Duration retryInitialBackoff) {
        this.retryInitialBackoff = retryInitialBackoff;
    }

    public Duration getMaxRetryAfter() {
        return maxRetryAfter;
    }

    public void setMaxRetryAfter(Duration maxRetryAfter) {
        this.maxRetryAfter = maxRetryAfter;
    }

    public ZoneId getMarketZone() {
        return marketZone;
    }

    public void setMarketZone(ZoneId marketZone) {
        this.marketZone = marketZone;
    }
}
