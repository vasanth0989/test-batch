package com.discover.billpay.migration.batch.service;

import com.discover.billpay.migration.batch.domain.ControlSummary;
import com.discover.billpay.migration.batch.exception.RecordValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ControlSummaryValidationService {

    public void validate(String falloutDetailFile, String controlSummaryFile) {
        var controlSummary = parse(controlSummaryFile);
        var actualFalloutRecords = countFalloutRecords(falloutDetailFile);

        if (controlSummary.getTotalInputRecords() != controlSummary.getSuccessRecords() + controlSummary.getFalloutRecords()) {
            var message = "Control summary validation failed: TotalInputRecords must equal SuccessRecords + FalloutRecords";
            log.error("{}; total={}, success={}, fallout={}",
                    message,
                    controlSummary.getTotalInputRecords(),
                    controlSummary.getSuccessRecords(),
                    controlSummary.getFalloutRecords());
            throw new RecordValidationException(message);
        }

        if (controlSummary.getFalloutRecords() != actualFalloutRecords) {
            var message = "Control summary validation failed: FalloutRecords does not match fallout detail file record count";
            log.error("{}; controlFalloutRecords={}, actualFalloutRecords={}",
                    message,
                    controlSummary.getFalloutRecords(),
                    actualFalloutRecords);
            throw new RecordValidationException(message);
        }

        log.info("Control summary validation passed for {}, falloutRecords={}",
                controlSummary.getInputFileName(),
                controlSummary.getFalloutRecords());
    }

    private ControlSummary parse(String controlSummaryFile) {
        var values = readKeyValues(controlSummaryFile);
        return ControlSummary.builder()
                .inputFileName(required(values, "InputFileName"))
                .totalInputRecords(requiredInt(values, "TotalInputRecords"))
                .successRecords(requiredInt(values, "SuccessRecords"))
                .falloutRecords(requiredInt(values, "FalloutRecords"))
                .build();
    }

    private Map<String, String> readKeyValues(String controlSummaryFile) {
        try (var lines = Files.lines(Path.of(controlSummaryFile))) {
            return lines
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> parts[0].trim(),
                            parts -> parts[1].trim(),
                            (left, right) -> right));
        } catch (IOException ex) {
            throw new RecordValidationException("Unable to read control summary file: " + controlSummaryFile);
        }
    }

    private int countFalloutRecords(String falloutDetailFile) {
        try (var lines = Files.lines(Path.of(falloutDetailFile))) {
            return Math.toIntExact(lines
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .count());
        } catch (IOException ex) {
            throw new RecordValidationException("Unable to read fallout detail file: " + falloutDetailFile);
        }
    }

    private String required(Map<String, String> values, String key) {
        return Optional.ofNullable(values.get(key))
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new RecordValidationException("Control summary missing required key: " + key));
    }

    private int requiredInt(Map<String, String> values, String key) {
        var value = required(values, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new RecordValidationException("Control summary key must be numeric: " + key);
        }
    }
}
