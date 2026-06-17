#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 input/finacle-fallout-report.psv input/finacle-control-summary.txt"
  exit 1
fi

FALLOUT_FILE="$1"
CONTROL_FILE="$2"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="output/recon-processing/${TIMESTAMP}"
RUN_ID="$(date +%s)"

mkdir -p input output archive error logs data

if [ ! -f "$FALLOUT_FILE" ]; then
  echo "Fallout detail file not found: $FALLOUT_FILE"
  exit 1
fi

if [ ! -f "$CONTROL_FILE" ]; then
  echo "Control summary file not found: $CONTROL_FILE"
  exit 1
fi

if [ ! -f build/libs/billpay-migration-batch-0.0.1-SNAPSHOT.jar ]; then
  if [ -f ./gradlew ]; then
    sh ./gradlew clean bootJar
  elif command -v gradle >/dev/null 2>&1; then
    gradle clean bootJar
  else
    echo "Gradle is required to build the jar. Install Gradle or add a Gradle wrapper, then rerun this script."
    exit 1
  fi
fi

java -jar build/libs/billpay-migration-batch-0.0.1-SNAPSHOT.jar \
  --spring.batch.job.name=reconProcessingJob \
  "inputFile=${FALLOUT_FILE}" \
  "controlFile=${CONTROL_FILE}" \
  "outputDir=${OUTPUT_DIR}" \
  "environment=local" \
  "runId=${RUN_ID}"

echo "Recon Processing output generated at:"
echo "${OUTPUT_DIR}/"
