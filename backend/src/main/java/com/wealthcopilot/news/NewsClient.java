package com.wealthcopilot.news;

import java.util.List;

/** Recent company news for the AI agent's get_stock_news tool. */
public interface NewsClient {

    /**
     * @return most recent articles for the ticker, newest first, possibly empty
     * @throws NewsClientException when the provider is unreachable or misconfigured
     */
    List<NewsArticle> fetchCompanyNews(String ticker);
}
