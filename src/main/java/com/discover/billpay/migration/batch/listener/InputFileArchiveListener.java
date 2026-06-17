package com.discover.billpay.migration.batch.listener;

import com.discover.billpay.migration.batch.config.MigrationProperties;
import com.discover.billpay.migration.batch.service.FileArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InputFileArchiveListener implements JobExecutionListener {

    private final MigrationProperties migrationProperties;
    private final FileArchiveService fileArchiveService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (!migrationProperties.isArchiveInputFiles() || jobExecution.getStatus() != BatchStatus.COMPLETED) {
            return;
        }

        archiveParameterFile(jobExecution, "inputFile");
        archiveParameterFile(jobExecution, "controlFile");
    }

    private void archiveParameterFile(JobExecution jobExecution, String parameterName) {
        var file = jobExecution.getJobParameters().getString(parameterName);
        if (file == null || file.isBlank()) {
            if ("inputFile".equals(parameterName)) {
                log.warn("Input file was not archived because inputFile job parameter is missing");
            }
            return;
        }

        fileArchiveService.archiveInputFile(file, jobExecution.getJobInstance().getJobName());
    }
}
