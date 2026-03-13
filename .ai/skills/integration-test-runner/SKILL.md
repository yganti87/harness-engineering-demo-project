---
name: integration-test-runner
description: Run Maven integration tests (Failsafe), capture raw output, and analyze output to determine success or failure. Use when running integration tests for plan execution, verifying features, or debugging integration test failures. Do NOT trust exit code alone—Failsafe can exit 0 despite test failures.
---

# Integration Test Runner

## Purpose

Run integration tests and **correctly determine success/failure** by parsing output. Maven Failsafe may exit 0 even when tests fail (e.g. `mvn failsafe:integration-test` without `failsafe:verify`). Always analyze the raw output.

## Workflow

1. **Run**: `cd backend && mvn failsafe:integration-test 2>&1`
2. **Capture**: Write full stdout+stderr to `docs/exec-plans/raw-test-output/{plan-id}-integration-raw.txt`
3. **Analyze**: Parse output to determine actual result (see Analysis rules below)
4. **Report**: Use the analyzed result (not exit code) for test summary and pass/fail decision

## Analysis Rules

**Success** when ALL of:
- Last aggregate line matches: `Tests run: N, Failures: 0, Errors: 0` (or `Skipped: N` only)
- No `[ERROR] Tests run:.*Failures: [1-9]` or `Errors: [1-9]` in aggregate
- No `<<< FAILURE!` or `<<< ERROR!` in test class summaries

**Failure** when ANY of:
- Aggregate line shows `Failures: [1-9]` or `Errors: [1-9]`
- Line contains `<<< FAILURE!` or `<<< ERROR!` for a test class
- Line contains `BUILD FAILURE`
- `[ERROR] Errors:` section lists failing tests

## Parsing the Output

**Recommended**: Run the analysis script after capturing output:
```bash
./.ai/scripts/analyze-maven-test-output.sh docs/exec-plans/raw-test-output/{plan-id}-integration-raw.txt
```
Script exits 0 on success, 1 on failure; prints one-line summary.

**Manual parsing** (if script unavailable): Search for these patterns:
1. **Final aggregate**: `Tests run: N, Failures: X, Errors: Y` — if Failures>0 or Errors>0 → FAILED
2. **Class-level failure**: `<<< FAILURE!` or `<<< ERROR!` → FAILED
3. **Build status**: `BUILD FAILURE` → FAILED

## Output for Test Summary

When failed, extract:
- Count: `Tests run: X, Failures: Y, Errors: Z`
- Failing tests: lines under `[ERROR] Errors:` or after `<<< ERROR!`
- Root cause: first `Caused by:` or `IllegalStateException` / `AssertionError` message

## Integration with Spec-Exec

The spec-exec agent MUST use this skill when running integration tests. After capturing raw output, run the analysis and base the test summary and pass/fail on the analysis result, not the process exit code.
