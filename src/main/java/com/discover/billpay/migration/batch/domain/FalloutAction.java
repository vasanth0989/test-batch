package com.discover.billpay.migration.batch.domain;

public enum FalloutAction {
    CLOSE_FUNDING_ACCOUNT,
    UNENROLL_CONSUMER,
    DELETE_CONSUMER,
    MANUAL_REVIEW,
    SKIP
}
