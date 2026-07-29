package com.wealthcopilot.news;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NewsConfiguration {

    @Bean
    public Cache<String, List<NewsArticle>> companyNewsCache(NewsProperties properties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtl())
                .maximumSize(500)
                .build();
    }
}
