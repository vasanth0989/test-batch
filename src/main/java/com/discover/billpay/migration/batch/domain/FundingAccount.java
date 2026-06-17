package com.discover.billpay.migration.batch.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FundingAccount {

    String accountNumber;
    String accountType;
    boolean primaryAccount;
}
