package com.discover.billpay.migration.batch.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReconResultRecord {

    String cif;
    String accountNumber;
    String falloutType;
    FalloutAction action;
    String message;
}
