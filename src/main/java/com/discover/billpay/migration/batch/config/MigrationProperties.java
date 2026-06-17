package com.discover.billpay.migration.batch.config;

import com.discover.billpay.migration.batch.domain.FalloutAction;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    private int chunkSize = 100;
    private int concurrency = 25;
    private int retryLimit = 3;
    private int skipLimit = 1000;
    private boolean archiveInputFiles = true;
    private Map<String, FalloutAction> falloutRules = new LinkedHashMap<>();
}
