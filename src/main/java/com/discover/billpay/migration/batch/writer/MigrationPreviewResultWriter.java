package com.discover.billpay.migration.batch.writer;

import com.discover.billpay.migration.batch.domain.MigrationErrorRecord;
import com.discover.billpay.migration.batch.domain.MigrationPreviewRecord;
import com.discover.billpay.migration.batch.domain.MigrationPreviewResult;
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
public class MigrationPreviewResultWriter implements ItemStreamWriter<MigrationPreviewResult> {

    private static final DateTimeFormatter OUTPUT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String PREVIEW_FILE_NAME = "migration-preview.psv";
    private static final String ERROR_FILE_NAME = "migration-errors.psv";

    private final String outputDir;
    private final String outputFile;
    private final String errorFile;
    private Path resolvedOutputDir;
    private Path resolvedOutputFile;
    private Path resolvedErrorFile;
    private BufferedWriter previewWriter;
    private BufferedWriter errorWriter;

    public MigrationPreviewResultWriter(
            @Value("#{jobParameters['outputDir']}") String outputDir,
            @Value("#{jobParameters['outputFile']}") String outputFile,
            @Value("#{jobParameters['errorFile']}") String errorFile) {
        this.outputDir = outputDir;
        this.outputFile = outputFile;
        this.errorFile = errorFile;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        try {
            resolvedOutputDir = resolveOutputDirectory();
            resolvedOutputFile = resolveFile(outputFile, PREVIEW_FILE_NAME);
            resolvedErrorFile = resolveFile(errorFile, ERROR_FILE_NAME);
            createParentDirectories(resolvedOutputFile);
            createParentDirectories(resolvedErrorFile);

            previewWriter = Files.newBufferedWriter(resolvedOutputFile, StandardCharsets.UTF_8);
            errorWriter = Files.newBufferedWriter(resolvedErrorFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to open migration preview output files", ex);
        }
    }

    @Override
    public synchronized void write(Chunk<? extends MigrationPreviewResult> chunk) {
        try {
            chunk.getItems().stream()
                    .flatMap(result -> result.getPreviewRecords().stream())
                    .map(this::formatPreviewRecord)
                    .forEach(this::writePreviewLine);

            chunk.getItems().stream()
                    .flatMap(result -> result.getErrorRecords().stream())
                    .map(this::formatErrorRecord)
                    .forEach(this::writeErrorLine);

            previewWriter.flush();
            errorWriter.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write migration preview output files", ex);
        }
    }

    @Override
    public void close() {
        closeQuietly(previewWriter);
        closeQuietly(errorWriter);
        log.info("Migration preview output written to {}", resolvedOutputFile);
        log.info("Migration preview errors written to {}", resolvedErrorFile);
        log.info("Migration Preview output generated at:\n{}", resolvedOutputDir);
    }

    private Path resolveOutputDirectory() {
        return hasText(outputDir)
                ? Path.of(outputDir)
                : Path.of("output", "migration-preview", LocalDateTime.now().format(OUTPUT_TIMESTAMP));
    }

    private Path resolveFile(String explicitFile, String fileName) {
        return hasText(explicitFile) ? Path.of(explicitFile) : resolvedOutputDir.resolve(fileName);
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

    private String formatPreviewRecord(MigrationPreviewRecord record) {
        return String.join("|",
                trim(record.getCif()),
                trim(record.getFiAccountNumber()),
                trim(record.getFiAccountType()),
                trim(record.getPrimaryAccountIndicator()));
    }

    private String formatErrorRecord(MigrationErrorRecord record) {
        return String.join("|",
                trim(record.getCif()),
                trim(record.getAccountNumber()),
                trim(record.getErrorCode()),
                trim(record.getErrorMessage()));
    }

    private void writePreviewLine(String line) {
        try {
            previewWriter.write(line);
            previewWriter.newLine();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write migration preview record", ex);
        }
    }

    private void writeErrorLine(String line) {
        try {
            errorWriter.write(line);
            errorWriter.newLine();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write migration error record", ex);
        }
    }

    private void closeQuietly(BufferedWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ex) {
            log.warn("Unable to close migration preview writer", ex);
        }
    }
}
