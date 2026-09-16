package com.kfokam48.exception;

/** Le fournisseur de taux externe est injoignable ou a renvoyé une réponse inexploitable. */
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
