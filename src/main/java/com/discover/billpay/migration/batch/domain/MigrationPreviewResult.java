package com.discover.billpay.migration.batch.domain;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MigrationPreviewResult {

    List<MigrationPreviewRecord> previewRecords;
    List<MigrationErrorRecord> errorRecords;
}
