package com.kfokam48.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configure le WebClient utilisé pour interroger l'API de taux externe.
 *
 * Spring WebClient est le client HTTP imposé par le cahier des charges.
 * L'URL de base est configurable (propriété app.exchange-api.base-url) afin de
 * pouvoir pointer vers un autre fournisseur ou un bouchon de test.
 */
@Configuration
public class WebClientConfig {

    @Value("${app.exchange-api.base-url:https://api.exchangerate-api.com}")
    private String baseUrl;

    @Bean
    public WebClient webClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();
    }
}
