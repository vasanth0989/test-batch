package com.discover.billpay.migration.batch.config;

import com.discover.billpay.migration.batch.domain.CifRecord;
import com.discover.billpay.migration.batch.domain.MigrationPreviewResult;
import com.discover.billpay.migration.batch.exception.BillPayApiException;
import com.discover.billpay.migration.batch.exception.RecordValidationException;
import com.discover.billpay.migration.batch.listener.InputFileArchiveListener;
import com.discover.billpay.migration.batch.listener.JobLoggingListener;
import com.discover.billpay.migration.batch.listener.SkipLoggingListener;
import com.discover.billpay.migration.batch.listener.StepLoggingListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.repeat.RepeatOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class MigrationPreviewJobConfig {

    private final MigrationProperties migrationProperties;
    private final JobLoggingListener jobLoggingListener;
    private final InputFileArchiveListener inputFileArchiveListener;
    private final StepLoggingListener stepLoggingListener;
    private final SkipLoggingListener skipLoggingListener;

    @Bean
    public Job migrationPreviewJob(JobRepository jobRepository, Step migrationPreviewStep) {
        return new JobBuilder("migrationPreviewJob", jobRepository)
                .listener(jobLoggingListener)
                .listener(inputFileArchiveListener)
                .start(migrationPreviewStep)
                .build();
    }

    @Bean
    public Step migrationPreviewStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<CifRecord> migrationPreviewReader,
            ItemProcessor<CifRecord, MigrationPreviewResult> migrationPreviewProcessor,
            ItemWriter<MigrationPreviewResult> migrationPreviewWriter,
            RepeatOperations migrationRepeatOperations) {

        return new StepBuilder("migrationPreviewStep", jobRepository)
                .<CifRecord, MigrationPreviewResult>chunk(migrationProperties.getChunkSize(), transactionManager)
                .reader(migrationPreviewReader)
                .processor(migrationPreviewProcessor)
                .writer(migrationPreviewWriter)
                .faultTolerant()
                .retry(BillPayApiException.class)
                .retryLimit(migrationProperties.getRetryLimit())
                .skip(FlatFileParseException.class)
                .skip(RecordValidationException.class)
                .skipLimit(migrationProperties.getSkipLimit())
                .listener(skipLoggingListener)
                .listener(stepLoggingListener)
                .chunkOperations(migrationRepeatOperations)
                .build();
    }
}
