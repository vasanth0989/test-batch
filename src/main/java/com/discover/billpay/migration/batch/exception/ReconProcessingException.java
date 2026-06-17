package com.discover.billpay.migration.batch.exception;

public class ReconProcessingException extends RuntimeException {

    public ReconProcessingException(String message) {
        super(message);
    }

    public ReconProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
