package com.discover.billpay.migration.batch.service.mock;

import com.discover.billpay.migration.batch.domain.ActionExecutionResult;
import com.discover.billpay.migration.batch.domain.FalloutAction;
import com.discover.billpay.migration.batch.service.BillPayEnrollmentService;
import com.discover.billpay.migration.batch.service.FundingAccountService;
import com.discover.billpay.migration.batch.service.ReconActionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockReconActionExecutor implements ReconActionExecutor {

    private final FundingAccountService fundingAccountService;
    private final BillPayEnrollmentService enrollmentService;

    @Override
    public ActionExecutionResult execute(
            String cif,
            String accountNumber,
            String fisAccountId,
            String falloutType,
            FalloutAction action) {
        // TODO Replace with real BillPay/FIS corrective action calls.
        try {
            return switch (action) {
                case CLOSE_FUNDING_ACCOUNT -> closeFundingAccount(cif, accountNumber, fisAccountId);
                case UNENROLL_CONSUMER -> unenrollConsumer(cif);
                case DELETE_CONSUMER -> deleteConsumer(cif);
                case MANUAL_REVIEW -> manualReview(falloutType);
                case SKIP -> skip(falloutType);
            };
        } catch (RuntimeException ex) {
            return ActionExecutionResult.builder()
                    .status("FAILED")
                    .message("Action failed: " + ex.getMessage())
                    .build();
        }
    }

    private ActionExecutionResult closeFundingAccount(String cif, String accountNumber, String fisAccountId) {
        fundingAccountService.deleteFundingAccount(cif, accountNumber, fisAccountId);
        return success("Funding account closed successfully");
    }

    private ActionExecutionResult unenrollConsumer(String cif) {
        enrollmentService.unenrollConsumer(cif);
        return success("Consumer unenrolled successfully");
    }

    private ActionExecutionResult deleteConsumer(String cif) {
        enrollmentService.deleteConsumer(cif);
        return success("Consumer deleted successfully");
    }

    private ActionExecutionResult manualReview(String falloutType) {
        log.info("MANUAL_REVIEW fallout type {} no action taken", falloutType);
        return ActionExecutionResult.builder()
                .status("MANUAL_REVIEW")
                .message("Manual review required")
                .build();
    }

    private ActionExecutionResult skip(String falloutType) {
        log.info("SKIP fallout type {} no action taken", falloutType);
        return success("Action skipped");
    }

    private ActionExecutionResult success(String message) {
        return ActionExecutionResult.builder()
                .status("SUCCESS")
                .message(message)
                .build();
    }
}
