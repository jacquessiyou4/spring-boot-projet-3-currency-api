package com.kfokam48.service;

import com.kfokam48.client.ExternalApiClient;
import com.kfokam48.dto.ExchangeRateDTO;
import com.kfokam48.entity.ExchangeRate;
import com.kfokam48.exception.RateNotFoundException;
import com.kfokam48.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Résolution et persistance des taux de change.
 *
 * Extrait de CurrencyConversionService : les annotations @Cacheable ne
 * s'appliquent qu'aux appels traversant le proxy Spring. Tant que getRate()
 * était appelée depuis une autre méthode du MÊME bean (auto-invocation), le
 * cache Redis n'était jamais consulté ni alimenté. En le plaçant dans un bean
 * distinct, chaque appel passe par le proxy et le cache fonctionne réellement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExternalApiClient externalApiClient;
    private final ExchangeRateRepository exchangeRateRepository;

    @Cacheable(value = "exchangeRates", key = "#from + '_' + #to")
    public BigDecimal getRate(String from, String to) {
        return resolveRate(from, to);
    }

    /** Force un appel au fournisseur externe et met le cache à jour avec la valeur fraîche. */
    @CachePut(value = "exchangeRates", key = "#from + '_' + #to")
    public BigDecimal refreshRate(String from, String to) {
        return resolveRate(from, to);
    }

    private BigDecimal resolveRate(String from, String to) {
        if (from.equals(to)) return BigDecimal.ONE;

        try {
            Map<String, Object> response = externalApiClient.getLatestRates(from);
            BigDecimal rate = extractRate(response, to);
            if (rate != null) {
                saveRateToDb(from, to, rate);
                return rate;
            }
            log.warn("Currency {} absent from external API response for base {}", to, from);
        } catch (Exception e) {
            log.warn("External API failed, trying database cache: {}", e.getMessage());
        }

        return getRateFromDb(from, to);
    }

    /**
     * Jackson désérialise un taux entier (ex. "JPY": 157) en Integer : un cast
     * direct vers Double provoquait une ClassCastException et donc un 404
     * silencieux sur ces devises. On passe par Number.
     */
    private BigDecimal extractRate(Map<String, Object> response, String to) {
        if (response == null) return null;
        Object ratesObject = response.get("rates");
        if (!(ratesObject instanceof Map<?, ?> rates)) return null;
        Object value = rates.get(to);
        if (!(value instanceof Number number)) return null;
        // On passe par la représentation textuelle : new BigDecimal(0.1d) vaudrait
        // 0.1000000000000000055511151231257827..., alors que new BigDecimal("0.1")
        // vaut exactement 0,1.
        return new BigDecimal(number.toString());
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDTO> getAllCachedRates() {
        return exchangeRateRepository.findAll().stream()
                .map(r -> new ExchangeRateDTO(r.getFromCurrency(), r.getToCurrency(), r.getRate()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExchangeRateDTO getRateDetail(String from, String to) {
        return exchangeRateRepository
                .findByFromCurrencyAndToCurrency(from.toUpperCase(), to.toUpperCase())
                .map(r -> new ExchangeRateDTO(r.getFromCurrency(), r.getToCurrency(), r.getRate()))
                .orElse(null);
    }

    private void saveRateToDb(String from, String to, BigDecimal rate) {
        ExchangeRate existing = exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to)
                .orElseGet(() -> ExchangeRate.builder().fromCurrency(from).toCurrency(to).build());
        existing.setRate(rate);
        existing.setExpiresAt(LocalDateTime.now().plusHours(24));
        exchangeRateRepository.save(existing);
    }

    private BigDecimal getRateFromDb(String from, String to) {
        ExchangeRate rate = exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to)
                .orElseThrow(() -> new RateNotFoundException(
                        "Exchange rate not found for " + from + " to " + to));
        if (rate.getExpiresAt() != null && rate.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Rate expired for {} to {}", from, to);
        }
        return rate.getRate();
    }
}
