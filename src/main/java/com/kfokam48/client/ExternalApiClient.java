package com.kfokam48.client;

import com.kfokam48.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

/**
 * Client d'accès au fournisseur de taux de change externe.
 *
 * Isole tout l'appel réseau dans une seule classe : le reste de l'application
 * ne connaît ni l'URL ni le format de réponse. Toute panne est convertie en
 * {@link com.kfokam48.exception.ExternalApiException}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalApiClient {

    private final WebClient webClient;

    public Map<String, Object> getLatestRates(String baseCurrency) {
        log.info("Fetching latest rates for currency: {}", baseCurrency);
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/v4/latest/{currency}", baseCurrency)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            log.info("Successfully fetched rates for {}", baseCurrency);
            return response;
        } catch (Exception e) {
            log.error("Error fetching rates from external API: {}", e.getMessage());
            throw new ExternalApiException("Failed to fetch exchange rates: " + e.getMessage(), e);
        }
    }
}
