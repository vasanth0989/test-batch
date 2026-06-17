package com.discover.billpay.migration.batch.service.mock;

import com.discover.billpay.migration.batch.domain.FundingAccount;
import com.discover.billpay.migration.batch.service.FundingAccountService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MockFundingAccountService implements FundingAccountService {

    @Override
    public List<FundingAccount> findFundingAccounts(String cif) {
        // TODO Replace with billpay-customer-api or funding account API lookup.
        return switch (lastCharacter(cif)) {
            case "1" -> List.of(
                    account(cif, "12345678901234567890", "DDA", true),
                    account(cif, "12345678901234567891", "SV", false));
            case "2" -> List.of(account(cif, "22345678901234567890", "DDA", true));
            default -> List.of(account(cif, "32345678901234567890", "DDA", true));
        };
    }

    private String lastCharacter(String value) {
        return value == null || value.isEmpty() ? "" : value.substring(value.length() - 1);
    }

    private FundingAccount account(String cif, String accountNumber, String accountType, boolean primaryAccount) {
        return FundingAccount.builder()
                .accountNumber(accountNumber)
                .accountType(accountType)
                .primaryAccount(primaryAccount)
                .build();
    }
}
