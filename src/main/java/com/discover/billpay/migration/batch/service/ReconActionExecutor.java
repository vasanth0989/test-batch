package com.discover.billpay.migration.batch.service;

import com.discover.billpay.migration.batch.domain.ActionExecutionResult;
import com.discover.billpay.migration.batch.domain.FalloutAction;

public interface ReconActionExecutor {

    ActionExecutionResult execute(String cif, String accountNumber, String falloutType, FalloutAction action);
}
