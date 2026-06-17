package com.discover.billpay.migration.batch.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ControlSummary {

    String inputFileName;
    int totalInputRecords;
    int successRecords;
    int falloutRecords;
}
