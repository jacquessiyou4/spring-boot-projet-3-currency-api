package com.kfokam48.repository;

import com.kfokam48.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès aux taux mémorisés en base, recherchés par couple de devises.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
