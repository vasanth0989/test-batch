package com.discover.billpay.migration.batch.tasklet;

import com.discover.billpay.migration.batch.exception.RecordValidationException;
import com.discover.billpay.migration.batch.service.ControlSummaryValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReconControlSummaryValidationTasklet implements Tasklet {

    private final ControlSummaryValidationService validationService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var jobParameters = stepExecution(chunkContext).getJobParameters();
        var falloutDetailFile = jobParameters.getString("inputFile");
        var controlSummaryFile = jobParameters.getString("controlFile");

        if (falloutDetailFile == null || falloutDetailFile.isBlank()) {
            throw new RecordValidationException("Recon processing requires fallout detail file parameter inputFile");
        }
        if (controlSummaryFile == null || controlSummaryFile.isBlank()) {
            throw new RecordValidationException("Recon processing requires control summary file parameter controlFile");
        }

        validationService.validate(falloutDetailFile, controlSummaryFile);
        return RepeatStatus.FINISHED;
    }

    private StepExecution stepExecution(ChunkContext chunkContext) {
        return chunkContext.getStepContext().getStepExecution();
    }
}
