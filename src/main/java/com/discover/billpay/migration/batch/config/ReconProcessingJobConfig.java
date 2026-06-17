package com.discover.billpay.migration.batch.config;

import com.discover.billpay.migration.batch.domain.ReconProcessingResult;
import com.discover.billpay.migration.batch.domain.ReconRecord;
import com.discover.billpay.migration.batch.exception.RecordValidationException;
import com.discover.billpay.migration.batch.exception.ReconProcessingException;
import com.discover.billpay.migration.batch.listener.InputFileArchiveListener;
import com.discover.billpay.migration.batch.listener.JobLoggingListener;
import com.discover.billpay.migration.batch.listener.SkipLoggingListener;
import com.discover.billpay.migration.batch.listener.StepLoggingListener;
import com.discover.billpay.migration.batch.tasklet.ReconControlSummaryValidationTasklet;
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
public class ReconProcessingJobConfig {

    private final MigrationProperties migrationProperties;
    private final JobLoggingListener jobLoggingListener;
    private final InputFileArchiveListener inputFileArchiveListener;
    private final StepLoggingListener stepLoggingListener;
    private final SkipLoggingListener skipLoggingListener;
    private final ReconControlSummaryValidationTasklet reconControlSummaryValidationTasklet;

    @Bean
    public Job reconProcessingJob(
            JobRepository jobRepository,
            Step reconControlSummaryValidationStep,
            Step reconProcessingStep) {

        return new JobBuilder("reconProcessingJob", jobRepository)
                .listener(jobLoggingListener)
                .listener(inputFileArchiveListener)
                .start(reconControlSummaryValidationStep)
                .next(reconProcessingStep)
                .build();
    }

    @Bean
    public Step reconControlSummaryValidationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("reconControlSummaryValidationStep", jobRepository)
                .tasklet(reconControlSummaryValidationTasklet, transactionManager)
                .listener(stepLoggingListener)
                .build();
    }

    @Bean
    public Step reconProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<ReconRecord> reconReader,
            ItemProcessor<ReconRecord, ReconProcessingResult> reconProcessor,
            ItemWriter<ReconProcessingResult> reconWriter,
            RepeatOperations migrationRepeatOperations) {

        return new StepBuilder("reconProcessingStep", jobRepository)
                .<ReconRecord, ReconProcessingResult>chunk(migrationProperties.getChunkSize(), transactionManager)
                .reader(reconReader)
                .processor(reconProcessor)
                .writer(reconWriter)
                .faultTolerant()
                .retry(ReconProcessingException.class)
                .retryLimit(migrationProperties.getRetryLimit())
                .skip(FlatFileParseException.class)
                .skip(RecordValidationException.class)
                .skip(ReconProcessingException.class)
                .skipLimit(migrationProperties.getSkipLimit())
                .listener(skipLoggingListener)
                .listener(stepLoggingListener)
                .chunkOperations(migrationRepeatOperations)
                .build();
    }
}
