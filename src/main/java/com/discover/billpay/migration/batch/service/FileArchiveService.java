package com.discover.billpay.migration.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileArchiveService {

    private static final DateTimeFormatter ARCHIVE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public void archiveInputFile(String inputFile, String jobName) {
        var source = Path.of(inputFile);
        if (!Files.exists(source)) {
            log.warn("Input file was not archived because it does not exist: {}", inputFile);
            return;
        }

        try {
            Files.createDirectories(Path.of("archive"));
            var archivedFile = Path.of("archive", archiveFileName(source, jobName));
            Files.move(source, archivedFile);
            log.info("Archived input file {} to {}", source, archivedFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to archive input file " + inputFile, ex);
        }
    }

    private String archiveFileName(Path source, String jobName) {
        var fileName = source.getFileName().toString();
        var timestamp = LocalDateTime.now().format(ARCHIVE_TIMESTAMP);
        return "%s-%s-%s".formatted(jobName, timestamp, fileName);
    }
}
