#!/usr/bin/env bash
# analyze-maven-test-output.sh: Parse Maven (Surefire/Failsafe) test output to determine success/failure.
# Usage: ./analyze-maven-test-output.sh <raw-output-file>
# Exit: 0 = success, 1 = failure
# Output: SUCCESS or FAILED, plus one-line summary (tests run, failures, errors)

set -euo pipefail

FILE="${1:?Usage: $0 <raw-output-file>}"
[[ -f "$FILE" ]] || { echo "FAILED: File not found: $FILE"; exit 1; }

# Check for aggregate line with failures or errors (Failures>0 or Errors>0)
if grep -qE 'Tests run: [0-9]+, Failures: [1-9][0-9]*|Tests run: [0-9]+, .* Errors: [1-9][0-9]*' "$FILE"; then
  AGGREGATE=$(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+' "$FILE" | tail -1)
  echo "FAILED: $AGGREGATE"
  exit 1
fi

# Check for <<< FAILURE! or <<< ERROR!
if grep -qE '<<< FAILURE!|<<< ERROR!' "$FILE"; then
  echo "FAILED: One or more tests failed or errored (see <<< FAILURE! / <<< ERROR! in output)"
  exit 1
fi

# Check for BUILD FAILURE
if grep -q 'BUILD FAILURE' "$FILE"; then
  echo "FAILED: BUILD FAILURE"
  exit 1
fi

# Success
AGGREGATE=$(grep -E 'Tests run: [0-9]+, Failures: 0, Errors: 0' "$FILE" | tail -1)
echo "SUCCESS: $AGGREGATE"
exit 0
