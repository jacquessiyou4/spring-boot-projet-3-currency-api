package com.kfokam48.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions centralisé.
 *
 * Met en œuvre la « gestion des erreurs » exigée par le cahier des charges :
 * 400 (devise invalide ou montant incorrect), 404 (taux introuvable),
 * 503 (panne de l'API externe).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRateNotFound(RateNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(404).body(error);
    }

    /** Le fournisseur externe est indisponible ET aucun taux n'est en base : 503, pas 500. */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<Map<String, Object>> handleExternalApi(ExternalApiException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 503);
        error.put("message", "Exchange rate provider is unavailable, please retry later");
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(503).body(error);
    }

    /** Violations de contraintes sur les variables de chemin (@Validated). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("status", 400);
        errors.put("timestamp", LocalDateTime.now());
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field;
            if (err instanceof FieldError fieldError) {
                field = fieldError.getField();
            } else {
                field = err.getObjectName();
            }
            fieldErrors.put(field, err.getDefaultMessage());
        });
        errors.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(errors);
    }
}