package com.kfokam48.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Taux de change entre deux devises, tel qu'exposé par l'API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDTO {
    private String fromCurrency;
    private String toCurrency;
    private Double rate;
}
