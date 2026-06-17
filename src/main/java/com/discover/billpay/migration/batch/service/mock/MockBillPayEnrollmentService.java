package com.discover.billpay.migration.batch.service.mock;

import com.discover.billpay.migration.batch.exception.BillPayApiException;
import com.discover.billpay.migration.batch.service.BillPayEnrollmentService;
import org.springframework.stereotype.Service;

@Service
public class MockBillPayEnrollmentService implements BillPayEnrollmentService {

    @Override
    public boolean isEnrolled(String cif) {
        // TODO Replace with billpay-customer-api enrollment lookup.
        if (cif.endsWith("8")) {
            throw new BillPayApiException("API_TIMEOUT", "Enrollment service timed out");
        }
        return !cif.endsWith("9");
    }
}
