package com.discover.billpay.migration.batch.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MigrationPreviewRecord {

    String cif;
    String fiAccountNumber;
    String fiAccountType;
    String primaryAccountIndicator;
}
