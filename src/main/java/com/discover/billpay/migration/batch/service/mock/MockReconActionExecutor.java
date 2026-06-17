package com.discover.billpay.migration.batch.service.mock;

import com.discover.billpay.migration.batch.domain.ActionExecutionResult;
import com.discover.billpay.migration.batch.domain.FalloutAction;
import com.discover.billpay.migration.batch.service.ReconActionExecutor;
import org.springframework.stereotype.Service;

@Service
public class MockReconActionExecutor implements ReconActionExecutor {

    @Override
    public ActionExecutionResult execute(String cif, String accountNumber, String falloutType, FalloutAction action) {
        // TODO Replace with real BillPay/FIS corrective action calls.
        if (accountNumber.endsWith("9999")) {
            return ActionExecutionResult.builder()
                    .message("Action failed: Mock action failed")
                    .build();
        }

        return ActionExecutionResult.builder()
                .message(successMessage(action))
                .build();
    }

    private String successMessage(FalloutAction action) {
        return switch (action) {
            case CLOSE_FUNDING_ACCOUNT -> "Funding account closed successfully";
            case UNENROLL_CONSUMER -> "Consumer unenrolled successfully";
            case DELETE_CONSUMER -> "Consumer deleted successfully";
            case SKIP -> "Action skipped";
            case MANUAL_REVIEW -> "Manual review required";
        };
    }
}
