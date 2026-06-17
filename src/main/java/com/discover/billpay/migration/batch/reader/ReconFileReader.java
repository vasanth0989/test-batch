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
                            .falloutType(fields.falloutType())
                            .rawLine(line)
                            .build();
                })
                .build();
    }

    @Override
    public synchronized ReconRecord read() throws Exception {
        return delegate.read();
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
        var fields = Optional.ofNullable(line)
                .map(value -> value.split("\\|", -1))
                .orElseGet(() -> new String[0]);
        if (fields.length != 3) {
            throw new RecordValidationException("Recon record must contain exactly 3 pipe-separated fields");
        }
        return new ReconFields(
                trim(field(fields, 0)),
                trim(field(fields, 1)),
                trim(field(fields, 2)));
    }

    private String field(String[] fields, int index) {
        return fields.length > index ? fields[index] : "";
    }

    private String trim(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }

    private record ReconFields(String cif, String accountNumber, String falloutType) {
    }
}
