package com.discover.billpay.migration.batch.processor;

import com.discover.billpay.migration.batch.config.MigrationProperties;
import com.discover.billpay.migration.batch.domain.ActionExecutionResult;
import com.discover.billpay.migration.batch.domain.FalloutAction;
import com.discover.billpay.migration.batch.domain.ManualReviewRecord;
import com.discover.billpay.migration.batch.domain.ReconProcessingResult;
import com.discover.billpay.migration.batch.domain.ReconRecord;
import com.discover.billpay.migration.batch.domain.ReconResultRecord;
import com.discover.billpay.migration.batch.service.ReconActionExecutor;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ReconProcessingItemProcessor implements ItemProcessor<ReconRecord, ReconProcessingResult> {

    private final MigrationProperties migrationProperties;
    private final ReconActionExecutor actionExecutor;

    @Override
    public ReconProcessingResult process(ReconRecord item) {
        var fields = Optional.ofNullable(item)
                .map(record -> new ReconFields(
                        trim(record.getCif()),
                        trim(record.getAccountNumber()),
                        trim(record.getFalloutType())))
                .orElseGet(ReconFields::empty);

        var validationMessage = validate(fields);
        if (validationMessage != null) {
            return manualReview(fields, "INVALID_RECORD", validationMessage);
        }

        var action = migrationProperties.getFalloutRules()
                .getOrDefault(fields.falloutType(), FalloutAction.MANUAL_REVIEW);

        if (action == FalloutAction.MANUAL_REVIEW) {
            return manualReview(fields, action.name(), "Manual review required");
        }

        try {
            var actionResult = actionExecutor.execute(
                    fields.cif(),
                    fields.accountNumber(),
                    fields.falloutType(),
                    action);
            return result(fields, action, actionResult.getMessage(), null);
        } catch (RuntimeException ex) {
            return manualReview(fields, action.name(), "Action failed: " + ex.getMessage());
        }
    }

    private String validate(ReconFields fields) {
        return switch (fields) {
            case ReconFields f when !StringUtils.hasText(f.cif()) -> "CIF is required";
            case ReconFields f when !StringUtils.hasText(f.accountNumber()) -> "Account number is required";
            case ReconFields f when !StringUtils.hasText(f.falloutType()) -> "Fallout type is required";
            default -> null;
        };
    }

    private ReconProcessingResult manualReview(
            ReconFields fields,
            String action,
            String reason) {

        var resolvedAction = resolveAction(action);
        var manualReviewRecord = ManualReviewRecord.builder()
                .cif(fields.cif())
                .accountNumber(fields.accountNumber())
                .falloutType(fields.falloutType())
                .reason(reason)
                .build();

        return result(fields, resolvedAction, reason, manualReviewRecord);
    }

    private ReconProcessingResult result(
            ReconFields fields,
            FalloutAction action,
            String message,
            ManualReviewRecord manualReviewRecord) {

        return ReconProcessingResult.builder()
                .reportRecord(ReconResultRecord.builder()
                        .cif(fields.cif())
                        .accountNumber(fields.accountNumber())
                        .falloutType(fields.falloutType())
                        .action(action)
                        .message(message)
                        .build())
                .manualReviewRecord(manualReviewRecord)
                .build();
    }

    private FalloutAction resolveAction(String action) {
        return Optional.ofNullable(action)
                .map(this::parseAction)
                .orElse(FalloutAction.MANUAL_REVIEW);
    }

    private String trim(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }

    private FalloutAction parseAction(String action) {
        try {
            return FalloutAction.valueOf(action);
        } catch (RuntimeException ex) {
            return FalloutAction.MANUAL_REVIEW;
        }
    }

    private record ReconFields(String cif, String accountNumber, String falloutType) {

        static ReconFields empty() {
            return new ReconFields("", "", "");
        }
    }
}
