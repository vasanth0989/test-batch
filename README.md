# BillPay Migration Batch

Spring Boot command-line batch utility for BillPay migration testing.

This utility implements `migrationPreviewJob` and `reconProcessingJob`. It does not include a REST API or a UI.

## Technology

- Java 21
- Gradle
- Spring Boot 4.1.0
- Spring Batch, managed by Spring Boot dependency management
- H2 in-memory database for Spring Batch metadata
- Lombok

## Build

```bash
gradle clean bootJar
```

The project includes a Gradle wrapper, so `./gradlew clean bootJar` works as well.

## Run Migration Preview

```bash
./scripts/run-migration-preview.sh input/sample-driver-file.txt
```

The script creates runtime folders if needed and writes stable file names inside a timestamped run directory:

- `output/migration-preview/<timestamp>/migration-preview.psv`
- `output/migration-preview/<timestamp>/migration-errors.psv`
- `logs/billpay-migration-batch.log`
- archived input under `archive/`

You can also run the jar directly:

```bash
java -jar build/libs/billpay-migration-batch-0.0.1-SNAPSHOT.jar \
  --spring.batch.job.name=migrationPreviewJob \
  inputFile=input/sample-driver-file.txt \
  outputDir=output/migration-preview/$(date +%Y%m%d-%H%M%S) \
  environment=local \
  runId=$(date +%s)
```

## Run Recon Processing

```bash
./scripts/run-recon-processing.sh input/sample-recon-file.psv input/sample-control-summary.txt
```

The script writes stable file names inside a timestamped run directory:

- `output/recon-processing/<timestamp>/recon-processing-report.psv`
- `output/recon-processing/<timestamp>/manual-review.psv`
- `logs/billpay-migration-batch.log`
- archived input under `archive/`

You can also run the jar directly:

```bash
java -jar build/libs/billpay-migration-batch-0.0.1-SNAPSHOT.jar \
  --spring.batch.job.name=reconProcessingJob \
  inputFile=input/sample-recon-file.psv \
  controlFile=input/sample-control-summary.txt \
  outputDir=output/recon-processing/$(date +%Y%m%d-%H%M%S) \
  environment=local \
  runId=$(date +%s)
```

## Input

Driver file contains one CIF per line:

```text
CIF1000001
CIF1000002
CIF1000008
CIF1000009
```

## Output

Output directories use timestamp format `yyyyMMdd-HHmmss`.

Migration preview output structure:

```text
output/migration-preview/<timestamp>/
  migration-preview.psv
  migration-errors.psv
```

Recon processing output structure:

```text
output/recon-processing/<timestamp>/
  recon-processing-report.psv
  manual-review.psv
```

Preview output is pipe-separated:

```text
CIF1000001|12345678901234567890|DDA|Y
CIF1000001|12345678901234567891|SV|N
```

Error output is pipe-separated:

```text
CIF1000008||API_TIMEOUT|Enrollment service timed out
CIF1000009||CONSUMER_NOT_FOUND|Consumer enrollment not found
```

Recon input is pipe-separated:

```text
CIF1000001|12345678901234567890|ACCOUNT_CLOSED
CIF1000002|22345678901234567890|FUNDING_ACCOUNT_MISSING
CIF1000003|32345678901234567890|UNKNOWN_STATE
```

Finacle must also provide a control summary file:

```text
InputFileName=migration-preview.psv
TotalInputRecords=100
SuccessRecords=95
FalloutRecords=5
```

The recon job validates the control summary before executing fallout actions:

- `TotalInputRecords` must equal `SuccessRecords + FalloutRecords`.
- `FalloutRecords` must equal the actual number of nonblank records in the fallout detail file.

If validation fails, `reconProcessingJob` fails and no fallout actions are executed.

Recon processing report output is pipe-separated:

```text
CIF1000001|12345678901234567890|ACCOUNT_CLOSED|UNENROLL_CONSUMER|Consumer unenrolled successfully
CIF1000002|22345678901234567890|FUNDING_ACCOUNT_MISSING|CLOSE_FUNDING_ACCOUNT|Funding account closed successfully
CIF1000003|32345678901234567890|UNKNOWN_STATE|MANUAL_REVIEW|Manual review required
```

Manual review output is pipe-separated:

```text
CIF1000003|32345678901234567890|UNKNOWN_STATE|Manual review required
```

## Mock BillPay Behavior

The mock services are deterministic:

- CIF ending in `1` returns two funding accounts.
- CIF ending in `2` returns one funding account.
- CIF ending in `8` simulates an API timeout.
- CIF ending in `9` simulates a missing enrollment.
- Other CIFs return one default DDA funding account.

Replace the mock implementations in `service/mock` with real BillPay API integrations later.

Recon actions are also mocked in `MockReconActionExecutor`.

## Configuration

`migration.chunk-size`, `migration.concurrency`, retry/skip limits, input archiving, and `migration.fallout-rules` are configured in `src/main/resources/application.yml`.

The jobs use bounded virtual-thread chunk execution. `migration.concurrency` limits concurrent chunk tasks so downstream mock or real API calls are not unbounded.

Completed jobs archive the processed input file into `archive/`. Set `migration.archive-input-files=false` to keep input files in place during local testing.

Spring Batch fault tolerance is configured with retry and skip limits. Record-level business failures continue to be written to the PSV outputs; malformed technical failures are skipped and logged by `SkipLoggingListener`.

Recon does not consume or create a success detail file. Finacle only needs to return the fallout detail file and the control summary file.

The current skeleton uses H2 in-memory metadata:

```yaml
spring.datasource.url: jdbc:h2:mem:billpay_migration_batch;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

For restartable shared testing, change this to a file database such as `jdbc:h2:file:./data/batchdb`.
