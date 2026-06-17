package com.discover.billpay.migration.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobLoggingListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting job {} with parameters {}", jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long readCount = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long writeCount = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        long skipCount = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum();

        log.info("Completed job {} with status {}, readCount={}, writeCount={}, skipCount={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                readCount,
                writeCount,
                skipCount);
    }
}
