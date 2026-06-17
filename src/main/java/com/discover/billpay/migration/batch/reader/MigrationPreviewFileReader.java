package com.discover.billpay.migration.batch.reader;

import com.discover.billpay.migration.batch.domain.CifRecord;
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

@Component("migrationPreviewReader")
public class MigrationPreviewFileReader implements ItemStreamReader<CifRecord>, StepExecutionListener {

    private FlatFileItemReader<CifRecord> delegate;
    private String inputFile;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        inputFile = stepExecution.getJobParameters().getString("inputFile");
        delegate = new FlatFileItemReaderBuilder<CifRecord>()
                .name("migrationPreviewReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper((line, lineNumber) -> CifRecord.builder()
                        .cif(line == null ? "" : line.trim())
                        .build())
                .build();
    }

    @Override
    public synchronized CifRecord read() throws Exception {
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
}
