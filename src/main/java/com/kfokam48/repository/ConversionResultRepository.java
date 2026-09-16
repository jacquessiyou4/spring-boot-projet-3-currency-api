package com.kfokam48.repository;

import com.kfokam48.entity.ConversionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Accès à l'historique des conversions, trié du plus récent au plus ancien.
 */
@Repository
public interface ConversionResultRepository extends JpaRepository<ConversionResult, Long> {
    List<ConversionResult> findTop10ByOrderByCreatedAtDesc();
    Page<ConversionResult> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
