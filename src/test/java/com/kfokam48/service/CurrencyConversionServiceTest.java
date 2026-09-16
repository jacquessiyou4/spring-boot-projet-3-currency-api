package com.kfokam48.service;

import com.kfokam48.dto.ConvertRequest;
import com.kfokam48.dto.ConvertResponse;
import com.kfokam48.dto.ExchangeRateDTO;
import com.kfokam48.entity.ConversionResult;
import com.kfokam48.repository.ConversionResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private ConversionResultRepository conversionResultRepository;

    @InjectMocks
    private CurrencyConversionService service;

    @Test
    void convert_shouldReturnConvertedAmount() {
        ConvertRequest request = new ConvertRequest("EUR", "USD", 100.0);

        when(exchangeRateService.getRate("EUR", "USD")).thenReturn(1.1235);
        when(conversionResultRepository.save(any(ConversionResult.class))).thenReturn(null);

        ConvertResponse result = service.convert(request);

        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo("EUR");
        assertThat(result.getToCurrency()).isEqualTo("USD");
        assertThat(result.getAmountFrom()).isEqualTo(100.0);
        assertThat(result.getAmountTo()).isCloseTo(112.35, org.assertj.core.data.Offset.offset(0.01));
        assertThat(result.getRate()).isCloseTo(1.1235, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void convert_sameCurrency_shouldReturnSameAmount() {
        ConvertRequest request = new ConvertRequest("USD", "USD", 50.0);
        when(exchangeRateService.getRate("USD", "USD")).thenReturn(1.0);

        ConvertResponse result = service.convert(request);

        assertThat(result.getAmountTo()).isEqualTo(50.0);
        assertThat(result.getRate()).isEqualTo(1.0);
    }

    @Test
    void refreshRate_shouldDelegateToExchangeRateService() {
        when(exchangeRateService.refreshRate("EUR", "USD")).thenReturn(1.15);

        ExchangeRateDTO result = service.refreshRate("eur", "usd");

        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo("EUR");
        assertThat(result.getToCurrency()).isEqualTo("USD");
        assertThat(result.getRate()).isEqualTo(1.15);
        verify(exchangeRateService).refreshRate("EUR", "USD");
    }

    @Test
    void getHistory_shouldReturnConversions() {
        ConversionResult cr = ConversionResult.builder()
                .fromCurrency("EUR").toCurrency("USD")
                .amountFrom(100.0).amountTo(112.0).rate(1.12).source("API")
                .createdAt(LocalDateTime.now()).build();
        when(conversionResultRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(Arrays.asList(cr)));

        List<ConvertResponse> result = service.getHistory(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSource()).isEqualTo("API");
    }

    @Test
    void clearHistory_shouldDeleteAll() {
        service.clearHistory();
        verify(conversionResultRepository).deleteAll();
    }
}
