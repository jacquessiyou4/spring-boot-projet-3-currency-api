package com.kfokam48.service;

import com.kfokam48.dto.ConvertRequest;
import com.kfokam48.dto.ConvertResponse;
import com.kfokam48.dto.ExchangeRateDTO;
import com.kfokam48.entity.ConversionResult;
import com.kfokam48.repository.ConversionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Couche Service : logique métier de la conversion.
 *
 * Normalise les devises, applique le taux fourni par ExchangeRateService,
 * journalise chaque conversion et expose l'historique.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;
    private static final int MAX_HISTORY_LIMIT = 200;

    private final ExchangeRateService exchangeRateService;
    private final ConversionResultRepository conversionResultRepository;

    @Transactional
    public ConvertResponse convert(ConvertRequest request) {
        String from = request.getFromCurrency().toUpperCase();
        String to = request.getToCurrency().toUpperCase();
        Double amount = request.getAmount();

        Double rate = exchangeRateService.getRate(from, to);
        Double result = amount * rate;

        conversionResultRepository.save(ConversionResult.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .amountFrom(amount)
                .amountTo(result)
                .rate(rate)
                .source("API")
                .build());

        return new ConvertResponse(from, to, amount, result, rate, "API", LocalDateTime.now());
    }

    public List<ExchangeRateDTO> getAllCachedRates() {
        return exchangeRateService.getAllCachedRates();
    }

    public ExchangeRateDTO getRateDetail(String from, String to) {
        return exchangeRateService.getRateDetail(from, to);
    }

    public ExchangeRateDTO refreshRate(String from, String to) {
        String normalizedFrom = from.toUpperCase();
        String normalizedTo = to.toUpperCase();
        Double rate = exchangeRateService.refreshRate(normalizedFrom, normalizedTo);
        return new ExchangeRateDTO(normalizedFrom, normalizedTo, rate);
    }

    @Transactional(readOnly = true)
    public List<ConvertResponse> getHistory(int limit) {
        if (limit < 1) limit = DEFAULT_HISTORY_LIMIT;
        if (limit > MAX_HISTORY_LIMIT) limit = MAX_HISTORY_LIMIT;
        return conversionResultRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(r -> new ConvertResponse(r.getFromCurrency(), r.getToCurrency(),
                        r.getAmountFrom(), r.getAmountTo(), r.getRate(), r.getSource(), r.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void clearHistory() {
        conversionResultRepository.deleteAll();
        log.info("Conversion history cleared");
    }
}
