package com.kfokam48.controller;

import com.kfokam48.dto.ConvertResponse;
import com.kfokam48.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de consultation et de purge de l'historique des conversions.
 */
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Tag(name = "Conversion History", description = "Historique des conversions")
public class HistoryController {

    private final CurrencyConversionService service;

    @GetMapping
    @Operation(summary = "Récupérer l'historique des conversions")
    public ResponseEntity<List<ConvertResponse>> getHistory(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.getHistory(limit));
    }

    @DeleteMapping
    @Operation(summary = "Supprimer tout l'historique")
    public ResponseEntity<Void> clearHistory() {
        service.clearHistory();
        return ResponseEntity.noContent().build();
    }
}
