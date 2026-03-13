---
name: unit-test-runner
description: Run Maven unit tests (Surefire), capture raw output, and analyze output to determine success or failure. Use when running unit tests for plan execution, verifying features, or debugging test failures. Analyze output in addition to exit code—exit code is usually reliable for Surefire but parsing ensures consistency with integration-test-runner.
---

# Unit Test Runner

## Purpose

Run unit tests (excluding integration) and determine success/failure by parsing output. Surefire typically fails the build (non-zero exit) on test failure, but always analyze output for consistency and to extract failure details.

## Workflow

1. **Run**: `cd backend && mvn test -Dgroups='!integration' 2>&1`
2. **Capture**: Write full stdout+stderr to `docs/exec-plans/raw-test-output/{plan-id}-unit-raw.txt`
3. **Analyze**: Parse output to determine actual result (see Analysis rules below)
4. **Report**: Use the analyzed result for test summary; if exit code was non-zero, treat as failure regardless of parsing

## Analysis Rules

**Success** when ALL of:
- Last aggregate line: `Tests run: N, Failures: 0, Errors: 0`
- No `<<< FAILURE!` or `<<< ERROR!` in test class lines
- No `BUILD FAILURE`
- Process exit code 0

**Failure** when ANY of:
- Exit code non-zero
- Aggregate shows `Failures: [1-9]` or `Errors: [1-9]`
- `<<< FAILURE!` or `<<< ERROR!` for a test class
- `BUILD FAILURE`

## Parsing the Output

**Recommended**: Run the analysis script after capturing output:
```bash
./.ai/scripts/analyze-maven-test-output.sh docs/exec-plans/raw-test-output/{plan-id}-unit-raw.txt
```
Script exits 0 on success, 1 on failure; prints one-line summary.

**Manual parsing** (if script unavailable): Search for Failures>0, Errors>0, `<<< FAILURE!`, `<<< ERROR!`, or `BUILD FAILURE`.

## Output for Test Summary

When failed, extract:
- `Tests run: X, Failures: Y, Errors: Z`
- Failing test classes and method names from `[ERROR]` lines
- First `Caused by:` or assertion message for root cause

## Integration with Spec-Exec

The spec-exec agent MUST use this skill when running unit tests. Capture raw output, then analyze. Base the test summary and pass/fail on both exit code and parsed result—if either indicates failure, report failure.
