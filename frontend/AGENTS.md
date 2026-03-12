# Frontend Agent Guide

> Read root AGENTS.md first, then docs/FRONTEND.md, then this file.

## Stack

- Python 3.11
- Streamlit 1.32.x
- `requests` library for backend API calls

## Key File

`app.py` — single-file Streamlit application. Keep as one file.

## Structure of app.py (in order)

1. Module docstring
2. Imports (stdlib then third-party)
3. Constants (`BACKEND_URL`, endpoints, `GENRE_OPTIONS`, `PAGE_SIZE`)
4. Logging setup (JSON to `/var/log/app/frontend.log`)
5. API helper functions
6. `st.set_page_config(...)` — **must be first Streamlit call**
7. Session state initialization (`st_` prefix for all keys)
8. UI rendering

## Session State Keys

| Key | Type | Purpose |
|-----|------|---------|
| `st_search_query` | str | Current search text |
| `st_genre_filter` | str | Selected genre |
| `st_current_page` | int | 0-indexed current page |

Always initialize session state keys before reading them.

## API Conventions

- All backend calls in helper functions
- Return `data` field (unwrapped from envelope) or `None` on error
- Call `st.error(...)` on failure with actionable message
- Log request and response at INFO level

## Running Locally (Without Docker)

```bash
cd frontend
pip install -r requirements.txt
BACKEND_URL=http://localhost:8080 streamlit run app.py --server.port 8501
```

## Test

```bash
cd frontend && python -m pytest tests/ -v
```

## Log Location

- Container: `/var/log/app/frontend.log`
- Host: `./logs/frontend/frontend.log`
