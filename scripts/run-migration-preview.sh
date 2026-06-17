#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 input/driver-file.txt"
  exit 1
fi

INPUT_FILE="$1"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="output/migration-preview/${TIMESTAMP}"
RUN_ID="$(date +%s)"

mkdir -p input output archive error logs data

if [ ! -f "$INPUT_FILE" ]; then
  echo "Input file not found: $INPUT_FILE"
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
  --spring.batch.job.name=migrationPreviewJob \
  "inputFile=${INPUT_FILE}" \
  "outputDir=${OUTPUT_DIR}" \
  "environment=local" \
  "runId=${RUN_ID}"

echo "Migration Preview output generated at:"
echo "${OUTPUT_DIR}/"
