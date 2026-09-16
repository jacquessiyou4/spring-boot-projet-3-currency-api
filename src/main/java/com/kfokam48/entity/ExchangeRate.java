package com.kfokam48.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_currency", "to_currency"})
})
/**
 * Entité JPA mémorisant le dernier taux connu pour un couple de devises.
 *
 * Sert de repli lorsque le fournisseur externe est injoignable, avec une date
 * d'expiration indicative.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false)
    private Double rate;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
