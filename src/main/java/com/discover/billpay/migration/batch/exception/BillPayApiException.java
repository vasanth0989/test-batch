package com.discover.billpay.migration.batch.exception;

import lombok.Getter;

@Getter
public class BillPayApiException extends RuntimeException {

    private final String errorCode;

    public BillPayApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
