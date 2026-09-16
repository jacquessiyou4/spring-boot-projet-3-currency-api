package com.kfokam48.integration;

import com.kfokam48.dto.ConvertRequest;
import com.kfokam48.entity.ConversionResult;
import com.kfokam48.entity.ExchangeRate;
import com.kfokam48.repository.ConversionResultRepository;
import com.kfokam48.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private ConversionResultRepository conversionResultRepository;

    @BeforeEach
    void setUp() {
        conversionResultRepository.deleteAll();
        exchangeRateRepository.deleteAll();
    }

    @Test
    void getHistory_shouldReturnEmptyInitially() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void clearHistory_shouldWork() throws Exception {
        ConversionResult cr = ConversionResult.builder()
                .fromCurrency("EUR").toCurrency("USD")
                .amountFrom(new BigDecimal("100")).amountTo(new BigDecimal("112.00")).rate(new BigDecimal("1.12")).source("API")
                .createdAt(LocalDateTime.now()).build();
        conversionResultRepository.save(cr);

        mockMvc.perform(delete("/history"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllCachedRates_shouldReturnEmptyInitially() throws Exception {
        mockMvc.perform(get("/convert/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getRateDetail_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/convert/rates/EUR/USD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRateDetail_afterSaving_shouldReturnRate() throws Exception {
        ExchangeRate rate = ExchangeRate.builder()
                .fromCurrency("EUR").toCurrency("USD").rate(new BigDecimal("1.12"))
                .expiresAt(LocalDateTime.now().plusHours(24)).build();
        exchangeRateRepository.save(rate);

        mockMvc.perform(get("/convert/rates/EUR/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate", is(1.12)));
    }

    @Test
    void getRatesFrom_shouldFilterByCurrency() throws Exception {
        exchangeRateRepository.save(ExchangeRate.builder()
                .fromCurrency("EUR").toCurrency("USD").rate(new BigDecimal("1.12"))
                .expiresAt(LocalDateTime.now().plusHours(24)).build());
        exchangeRateRepository.save(ExchangeRate.builder()
                .fromCurrency("GBP").toCurrency("USD").rate(new BigDecimal("1.30"))
                .expiresAt(LocalDateTime.now().plusHours(24)).build());

        mockMvc.perform(get("/convert/rates/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fromCurrency", is("EUR")));
    }

    @Test
    void convert_withInvalidData_shouldReturn400() throws Exception {
        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromCurrency\": \"\", \"amount\": -1}"))
                .andExpect(status().isBadRequest());
    }
}
