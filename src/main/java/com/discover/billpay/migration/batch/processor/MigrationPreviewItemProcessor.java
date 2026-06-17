package com.discover.billpay.migration.batch.processor;

import com.discover.billpay.migration.batch.domain.CifRecord;
import com.discover.billpay.migration.batch.domain.FundingAccount;
import com.discover.billpay.migration.batch.domain.MigrationErrorRecord;
import com.discover.billpay.migration.batch.domain.MigrationPreviewRecord;
import com.discover.billpay.migration.batch.domain.MigrationPreviewResult;
import com.discover.billpay.migration.batch.exception.BillPayApiException;
import com.discover.billpay.migration.batch.service.BillPayEnrollmentService;
import com.discover.billpay.migration.batch.service.FundingAccountService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MigrationPreviewItemProcessor implements ItemProcessor<CifRecord, MigrationPreviewResult> {

    private final BillPayEnrollmentService enrollmentService;
    private final FundingAccountService fundingAccountService;

    @Override
    public MigrationPreviewResult process(CifRecord item) {
        var cif = Optional.ofNullable(item)
                .map(CifRecord::getCif)
                .map(this::trim)
                .orElse("");

        if (!StringUtils.hasText(cif)) {
            return error(cif, "BLANK_CIF", "CIF is required");
        }

        try {
            if (!enrollmentService.isEnrolled(cif)) {
                return error(cif, "CONSUMER_NOT_FOUND", "Consumer enrollment not found");
            }

            var accounts = fundingAccountService.findFundingAccounts(cif);
            if (accounts.isEmpty()) {
                return error(cif, "FUNDING_ACCOUNT_NOT_FOUND", "Funding account not found");
            }

            var previewRecords = accounts.stream()
                    .map(account -> toPreviewRecord(cif, account))
                    .toList();

            return MigrationPreviewResult.builder()
                    .previewRecords(previewRecords)
                    .errorRecords(List.of())
                    .build();
        } catch (BillPayApiException ex) {
            return error(cif, ex.getErrorCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            return error(cif, "PROCESSING_ERROR", "Unable to process CIF");
        }
    }

    private MigrationPreviewRecord toPreviewRecord(String cif, FundingAccount account) {
        return MigrationPreviewRecord.builder()
                .cif(cif)
                .fiAccountNumber(trim(account.getAccountNumber()))
                .fiAccountType(trim(account.getAccountType()))
                .primaryAccountIndicator(account.isPrimaryAccount() ? "Y" : "N")
                .build();
    }

    private MigrationPreviewResult error(String cif, String errorCode, String errorMessage) {
        return MigrationPreviewResult.builder()
                .previewRecords(List.of())
                .errorRecords(List.of(MigrationErrorRecord.builder()
                        .cif(trim(cif))
                        .accountNumber("")
                        .errorCode(trim(errorCode))
                        .errorMessage(trim(errorMessage))
                        .build()))
                .build();
    }

    private String trim(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }
}
