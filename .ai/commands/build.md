---
description: Build all Docker images and report any compilation errors
---

Build Docker images for all services without starting them.

## Steps

1. Run `./scripts/build.sh` from the project root
2. Monitor for compilation errors
3. Report:
   - Which images built successfully
   - Any Java compilation errors (file name + line number)
   - Any missing dependencies or classpath errors
4. If successful: "Build successful. Run ./scripts/start.sh to start."
5. If failed: Show the error and suggest the fix

## Commands

```bash
# Build all images
./scripts/build.sh

# Build and start immediately
./scripts/start.sh

# Backend compilation only (faster feedback)
cd backend && mvn compile

# Validate (Checkstyle + compilation)
cd backend && mvn validate && mvn compile

# Rebuild only backend after code changes (faster than full build)
docker compose build backend && docker compose up -d backend
```

## Common Build Errors

| Error | Fix |
|-------|-----|
| `package com.library.X does not exist` | Check import path — class may be in wrong package or not yet created |
| `cannot find symbol` | Missing import or wrong type. Check `docs/PATTERNS.md` for the correct type |
| `checkstyle violation` | Run `mvn checkstyle:check` and fix. See `docs/CODING_STYLE.md` |
| `Flyway migration checksum mismatch` | NEVER edit applied migrations. Create `V{n+1}__new_migration.sql` instead |
| `org.mapstruct: no source files` | Add `@Mapper` annotation and implement the mapper interface |
