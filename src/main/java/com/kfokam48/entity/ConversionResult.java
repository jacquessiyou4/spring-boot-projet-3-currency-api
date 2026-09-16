package com.kfokam48.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité JPA journalisant chaque conversion effectuée (table « conversion_history »).
 *
 * Permet de retracer l'historique demandé par l'API.
 */
@Entity
@Table(name = "conversion_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amountFrom;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amountTo;

    @Column(nullable = false, precision = 19, scale = 10)
    private BigDecimal rate;

    @Column(nullable = false, length = 20)
    private String source;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
