package com.kfokam48.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requête de conversion : devise source, devise cible et montant.
 *
 * Les codes devise sont validés par expression régulière (3 lettres, norme
 * ISO 4217) et le montant doit être strictement positif.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertRequest {

    @NotBlank(message = "Source currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Source currency must be a 3-letter ISO code")
    private String fromCurrency;

    @NotBlank(message = "Target currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Target currency must be a 3-letter ISO code")
    private String toCurrency;

    /**
     * Montant à convertir. BigDecimal et non double : un montant monétaire ne
     * doit jamais transiter par une virgule flottante binaire, qui ne peut pas
     * représenter exactement des valeurs comme 0,10 et accumule des écarts.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
}
