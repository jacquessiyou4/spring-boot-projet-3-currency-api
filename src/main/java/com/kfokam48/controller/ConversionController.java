package com.kfokam48.controller;

import com.kfokam48.dto.ConvertRequest;
import com.kfokam48.dto.ConvertResponse;
import com.kfokam48.dto.ExchangeRateDTO;
import com.kfokam48.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de conversion et de consultation des taux.
 *
 * POST /convert réalise la conversion demandée (devise source, devise cible,
 * montant) ; les routes /convert/rates exposent les taux connus et permettent
 * de les rafraîchir depuis le fournisseur externe.
 */
@RestController
@Validated
@RequestMapping("/convert")
@RequiredArgsConstructor
@Tag(name = "Currency Conversion", description = "Conversion de devises")
public class ConversionController {

    private final CurrencyConversionService service;

    @PostMapping
    @Operation(summary = "Convertir un montant entre devises")
    public ResponseEntity<ConvertResponse> convert(@Valid @RequestBody ConvertRequest request) {
        return ResponseEntity.ok(service.convert(request));
    }

    @GetMapping("/rates")
    @Operation(summary = "Récupérer tous les taux en cache")
    public ResponseEntity<List<ExchangeRateDTO>> getAllRates() {
        return ResponseEntity.ok(service.getAllCachedRates());
    }

    @GetMapping("/rates/{from}")
    @Operation(summary = "Récupérer les taux depuis une devise")
    public ResponseEntity<List<ExchangeRateDTO>> getRatesFrom(
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code") String from) {
        List<ExchangeRateDTO> rates = service.getAllCachedRates().stream()
                .filter(r -> r.getFromCurrency().equalsIgnoreCase(from))
                .toList();
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/rates/{from}/{to}")
    @Operation(summary = "Récupérer le taux entre deux devises")
    public ResponseEntity<ExchangeRateDTO> getRate(
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code") String from,
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code") String to) {
        ExchangeRateDTO rate = service.getRateDetail(from, to);
        if (rate == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(rate);
    }

    @PutMapping("/rates/{from}/{to}")
    @Operation(summary = "Actualiser le taux depuis l'API externe")
    public ResponseEntity<ExchangeRateDTO> refreshRate(
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code") String from,
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code") String to) {
        return ResponseEntity.ok(service.refreshRate(from, to));
    }
}
