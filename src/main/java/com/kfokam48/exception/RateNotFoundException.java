package com.kfokam48.exception;

/**
 * Levée quand aucun taux n'est disponible pour le couple demandé, ni chez le
 * fournisseur ni en base → HTTP 404.
 */
public class RateNotFoundException extends RuntimeException {
    public RateNotFoundException(String message) {
        super(message);
    }
}