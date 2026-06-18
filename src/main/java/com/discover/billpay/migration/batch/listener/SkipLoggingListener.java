package com.discover.billpay.migration.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SkipLoggingListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable throwable) {
        if (throwable instanceof FlatFileParseException parseException && parseException.getCause() != null) {
            log.warn(
                    "Skipped record during read at line {}: {}. Input: {}",
                    parseException.getLineNumber(),
                    parseException.getCause().getMessage(),
                    parseException.getInput());
            return;
        }

        log.warn("Skipped record during read: {}", throwable.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable throwable) {
        log.warn("Skipped record during processing: {}", throwable.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable throwable) {
        log.warn("Skipped record during write: {}", throwable.getMessage());
    }
}
