# Task: Bring types/dto quality score to A

**Task ID**: T001  
**Status**: completed  
**Scope**: backend

---

## Context

The `types/dto` module is currently graded B in [docs/QUALITY_SCORE.md](../QUALITY_SCORE.md): "DTOs defined. No tests needed (data classes)." To reach A (Production-ready. Full test coverage, no known issues.), we need unit tests covering DTO behavior: static factories, validation rules, and critical logic.

---

## Description

- [x] Add `src/test/java/com/library/unit/types/dto/ApiResponseTest.java` — test `success()` and `error()` factory methods
- [x] Add `src/test/java/com/library/unit/types/dto/PagedResponseTest.java` — test `of(Page)` static factory
- [x] Add `src/test/java/com/library/unit/types/dto/BookSearchRequestValidationTest.java` — test `@Size` and `@Pattern` validation (valid and invalid inputs)
- [ ] Add minimal tests for `BookDto` builder / Lombok behavior (optional; skipped — covered by integration tests)
- [x] Update `docs/QUALITY_SCORE.md`: change `types/dto` grade from B to A and update Notes

---

## Acceptance Criteria

1. `ApiResponse.success(data)` returns 200, populates data and timestamp, error is null
2. `ApiResponse.error(statusCode, message)` returns given status, populates error message, data is null
3. `PagedResponse.of(Page)` correctly maps page number, size, totalElements, totalPages, last, content
4. `BookSearchRequest` validation rejects: q longer than 200 chars, q with disallowed characters; accepts valid q and genre
5. Unit tests pass: `cd backend && mvn test -Dgroups='!integration'`
6. Checkstyle passes: `cd backend && mvn checkstyle:check`
7. `docs/QUALITY_SCORE.md` shows `types/dto` with grade A and note reflecting test coverage

---

## Verification

| Check | Command / Action |
|-------|------------------|
| Checkstyle | `cd backend && mvn checkstyle:check` |
| Unit tests | `cd backend && mvn test -Dgroups='!integration'` |
| Manual | Confirm `docs/QUALITY_SCORE.md` has types/dto at A |

---

## Out of Scope

- JSON serialization tests (covered indirectly by integration tests)
- Changes to DTO implementation (only add tests)
- Integration tests (DTOs exercised via existing integration tests)

---

## Files Touched (Expected)

- `backend/src/test/java/com/library/unit/types/dto/ApiResponseTest.java` (new)
- `backend/src/test/java/com/library/unit/types/dto/PagedResponseTest.java` (new)
- `backend/src/test/java/com/library/unit/types/dto/BookSearchRequestValidationTest.java` (new)
- `docs/QUALITY_SCORE.md` (update types/dto row)
