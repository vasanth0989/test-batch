package com.discover.billpay.migration.batch.reader;

import com.discover.billpay.migration.batch.domain.ReconRecord;
import com.discover.billpay.migration.batch.exception.RecordValidationException;
import java.util.Optional;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("reconReader")
public class ReconFileReader implements ItemStreamReader<ReconRecord>, StepExecutionListener {

    private FlatFileItemReader<ReconRecord> delegate;
    private String inputFile;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        inputFile = stepExecution.getJobParameters().getString("inputFile");
        delegate = new FlatFileItemReaderBuilder<ReconRecord>()
                .name("reconReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper((line, lineNumber) -> {
                    var fields = parse(line);
                    return ReconRecord.builder()
                            .cif(fields.cif())
                            .accountNumber(fields.accountNumber())
                            .fisAccountId(fields.fisAccountId())
                            .falloutType(fields.falloutType())
                            .rawLine(line)
                            .build();
                })
                .build();
    }

    @Override
    public synchronized ReconRecord read() throws Exception {
        ReconRecord record;
        do {
            record = delegate.read();
        } while (record != null && !StringUtils.hasText(record.getRawLine()));
        return record;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    private ReconFields parse(String line) {
        if (line == null || line.isBlank()) {
            return ReconFields.empty();
        }

        var fields = Optional.ofNullable(line)
                .map(value -> value.split("\\|", -1))
                .orElseGet(() -> new String[0]);
        if (fields.length != 4) {
            throw new RecordValidationException(
                    "Recon record must contain exactly 4 pipe-separated fields in layout "
                            + "CIF|AccountNumber|FisAccountId|FalloutType, but found "
                            + fields.length
                            + " field(s)");
        }
        return new ReconFields(
                trim(field(fields, 0)),
                trim(field(fields, 1)),
                trim(field(fields, 2)),
                trim(field(fields, 3)));
    }

    private String field(String[] fields, int index) {
        return fields.length > index ? fields[index] : "";
    }

    private String trim(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }

    private record ReconFields(String cif, String accountNumber, String fisAccountId, String falloutType) {

        static ReconFields empty() {
            return new ReconFields("", "", "", "");
        }
    }
}
