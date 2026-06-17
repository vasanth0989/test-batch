package com.discover.billpay.migration.batch.service;

import com.discover.billpay.migration.batch.domain.FundingAccount;
import java.util.List;

public interface FundingAccountService {

    List<FundingAccount> findFundingAccounts(String cif);
}
