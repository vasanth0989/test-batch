package com.discover.billpay.migration.batch.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReconRecord {

    String cif;
    String accountNumber;
    String falloutType;
    String rawLine;
}
