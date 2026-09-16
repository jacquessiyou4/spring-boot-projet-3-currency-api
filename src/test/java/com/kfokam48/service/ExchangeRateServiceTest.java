package com.kfokam48.service;

import com.kfokam48.client.ExternalApiClient;
import com.kfokam48.dto.ExchangeRateDTO;
import com.kfokam48.entity.ExchangeRate;
import com.kfokam48.exception.ExternalApiException;
import com.kfokam48.exception.RateNotFoundException;
import com.kfokam48.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExternalApiClient externalApiClient;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateService service;

    private Map<String, Object> apiResponse(String currency, Object rate) {
        Map<String, Object> rates = new HashMap<>();
        rates.put(currency, rate);
        Map<String, Object> response = new HashMap<>();
        response.put("rates", rates);
        return response;
    }

    @Test
    void getRate_shouldReturnRateFromApiAndPersistIt() {
        when(externalApiClient.getLatestRates("EUR")).thenReturn(apiResponse("USD", 1.1235));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "USD"))
                .thenReturn(Optional.empty());

        assertThat(service.getRate("EUR", "USD")).isEqualByComparingTo("1.1235");
        verify(exchangeRateRepository).save(any(ExchangeRate.class));
    }

    /**
     * Régression : Jackson renvoie un Integer pour un taux entier ("JPY": 157).
     * L'ancien cast (Map<String, Double>) levait une ClassCastException, avalée
     * par le catch, ce qui transformait ces devises en 404 alors que l'API
     * externe avait bien répondu.
     */
    @Test
    void getRate_withIntegerRateFromApi_shouldNotFallBackToDb() {
        when(externalApiClient.getLatestRates("EUR")).thenReturn(apiResponse("JPY", 157));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "JPY"))
                .thenReturn(Optional.empty());

        assertThat(service.getRate("EUR", "JPY")).isEqualByComparingTo("157");
        verify(exchangeRateRepository).save(any(ExchangeRate.class));
    }

    @Test
    void getRate_sameCurrency_shouldReturnOne() {
        assertThat(service.getRate("USD", "USD")).isEqualByComparingTo("1");
        verifyNoInteractions(externalApiClient);
    }

    @Test
    void getRate_whenApiFails_shouldFallbackToDb() {
        when(externalApiClient.getLatestRates("EUR"))
                .thenThrow(new ExternalApiException("API down", null));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "USD"))
                .thenReturn(Optional.of(ExchangeRate.builder()
                        .fromCurrency("EUR").toCurrency("USD").rate(new BigDecimal("1.10")).build()));

        assertThat(service.getRate("EUR", "USD")).isEqualByComparingTo("1.10");
    }

    @Test
    void getRate_whenApiFailsAndNotInDb_shouldThrow() {
        when(externalApiClient.getLatestRates("EUR"))
                .thenThrow(new ExternalApiException("API down", null));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "USD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate("EUR", "USD"))
                .isInstanceOf(RateNotFoundException.class)
                .hasMessageContaining("Exchange rate not found");
    }

    @Test
    void getRate_whenTargetCurrencyMissingFromResponse_shouldFallbackToDb() {
        when(externalApiClient.getLatestRates("EUR")).thenReturn(apiResponse("GBP", 0.85));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "USD"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate("EUR", "USD"))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void getAllCachedRates_shouldReturnList() {
        when(exchangeRateRepository.findAll()).thenReturn(Arrays.asList(
                ExchangeRate.builder().fromCurrency("EUR").toCurrency("USD").rate(new BigDecimal("1.12")).build(),
                ExchangeRate.builder().fromCurrency("GBP").toCurrency("USD").rate(new BigDecimal("1.30")).build()));

        List<ExchangeRateDTO> result = service.getAllCachedRates();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRate()).isEqualByComparingTo("1.12");
    }

    /**
     * Un taux entier ou à décimales « rondes » doit être conservé exactement :
     * c'est précisément ce que la virgule flottante ne garantit pas.
     */
    @Test
    void getRate_shouldPreserveDecimalValueExactly() {
        when(externalApiClient.getLatestRates("EUR")).thenReturn(apiResponse("CHF", 0.1));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "CHF"))
                .thenReturn(Optional.empty());

        assertThat(service.getRate("EUR", "CHF")).isEqualByComparingTo(new BigDecimal("0.1"));
    }

    @Test
    void getRateDetail_whenAbsent_shouldReturnNull() {
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency("EUR", "USD"))
                .thenReturn(Optional.empty());

        assertThat(service.getRateDetail("eur", "usd")).isNull();
    }
}
