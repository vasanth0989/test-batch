package com.discover.billpay.migration.batch.exception;

public class RecordValidationException extends RuntimeException {

    public RecordValidationException(String message) {
        super(message);
    }
}
