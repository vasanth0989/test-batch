package com.discover.billpay.migration.batch.domain;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReconProcessingResult {

    ReconResultRecord reportRecord;
    ManualReviewRecord manualReviewRecord;

    public Optional<ManualReviewRecord> manualReviewRecord() {
        return Optional.ofNullable(manualReviewRecord);
    }
}
