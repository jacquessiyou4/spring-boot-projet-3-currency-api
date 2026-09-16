package com.kfokam48.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Résultat d'une conversion : montants, taux appliqué, origine et horodatage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertResponse {
    private String fromCurrency;
    private String toCurrency;
    private Double amountFrom;
    private Double amountTo;
    private Double rate;
    private String source;
    private LocalDateTime timestamp;
}
