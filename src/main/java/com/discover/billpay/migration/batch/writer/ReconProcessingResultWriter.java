package com.discover.billpay.migration.batch.writer;

import com.discover.billpay.migration.batch.domain.ManualReviewRecord;
import com.discover.billpay.migration.batch.domain.ReconProcessingResult;
import com.discover.billpay.migration.batch.domain.ReconResultRecord;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class ReconProcessingResultWriter implements ItemStreamWriter<ReconProcessingResult> {

    private static final DateTimeFormatter OUTPUT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String REPORT_FILE_NAME = "recon-processing-report.psv";
    private static final String MANUAL_REVIEW_FILE_NAME = "manual-review.psv";

    private final String outputDir;
    private final String reportFile;
    private final String manualReviewFile;
    private Path resolvedOutputDir;
    private Path resolvedReportFile;
    private Path resolvedManualReviewFile;
    private BufferedWriter reportWriter;
    private BufferedWriter manualReviewWriter;

    public ReconProcessingResultWriter(
            @Value("#{jobParameters['outputDir']}") String outputDir,
            @Value("#{jobParameters['reportFile']}") String reportFile,
            @Value("#{jobParameters['manualReviewFile']}") String manualReviewFile) {
        this.outputDir = outputDir;
        this.reportFile = reportFile;
        this.manualReviewFile = manualReviewFile;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        try {
            resolvedOutputDir = resolveOutputDirectory();
            resolvedReportFile = resolveFile(reportFile, REPORT_FILE_NAME);
            resolvedManualReviewFile = resolveFile(manualReviewFile, MANUAL_REVIEW_FILE_NAME);
            createParentDirectories(resolvedReportFile);
            createParentDirectories(resolvedManualReviewFile);

            reportWriter = Files.newBufferedWriter(resolvedReportFile, StandardCharsets.UTF_8);
            manualReviewWriter = Files.newBufferedWriter(resolvedManualReviewFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to open recon output files", ex);
        }
    }

    @Override
    public synchronized void write(Chunk<? extends ReconProcessingResult> chunk) {
        try {
            chunk.getItems().stream()
                    .map(ReconProcessingResult::getReportRecord)
                    .map(this::formatReportRecord)
                    .forEach(this::writeReportLine);

            chunk.getItems().stream()
                    .map(ReconProcessingResult::manualReviewRecord)
                    .flatMap(Optional::stream)
                    .map(this::formatManualReviewRecord)
                    .forEach(this::writeManualReviewLine);

            reportWriter.flush();
            manualReviewWriter.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write recon output files", ex);
        }
    }

    @Override
    public void close() {
        closeQuietly(reportWriter);
        closeQuietly(manualReviewWriter);
        log.info("Recon processing report written to {}", resolvedReportFile);
        log.info("Manual review records written to {}", resolvedManualReviewFile);
        log.info("Recon Processing output generated at:\n{}", resolvedOutputDir);
    }

    private Path resolveOutputDirectory() {
        return hasText(outputDir)
                ? Path.of(outputDir)
                : Path.of("output", "recon-processing", LocalDateTime.now().format(OUTPUT_TIMESTAMP));
    }

    private Path resolveFile(String explicitFile, String fileName) {
        return hasText(explicitFile) ? Path.of(explicitFile) : resolvedOutputDir.resolve(fileName);
    }

    private String formatReportRecord(ReconResultRecord report) {
        return String.join("|",
                trim(report.getCif()),
                trim(report.getAccountNumber()),
                trim(report.getFalloutType()),
                enumName(report.getAction()),
                trim(report.getMessage()));
    }

    private String formatManualReviewRecord(ManualReviewRecord record) {
        return String.join("|",
                trim(record.getCif()),
                trim(record.getAccountNumber()),
                trim(record.getFalloutType()),
                trim(record.getReason()));
    }

    private void writeReportLine(String line) {
        try {
            reportWriter.write(line);
            reportWriter.newLine();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write recon report record", ex);
        }
    }

    private void writeManualReviewLine(String line) {
        try {
            manualReviewWriter.write(line);
            manualReviewWriter.newLine();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write manual review record", ex);
        }
    }

    private void createParentDirectories(Path path) throws IOException {
        var parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String trim(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String enumName(Enum<?> value) {
        return switch (value) {
            case null -> "";
            default -> value.name();
        };
    }

    private void closeQuietly(BufferedWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ex) {
            log.warn("Unable to close recon writer", ex);
        }
    }
}
