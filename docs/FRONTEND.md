# Frontend Guide (Streamlit)

## Stack

- Python 3.11
- Streamlit 1.32.x
- `requests` for HTTP calls to backend

## File Structure

`frontend/app.py` is a single-file Streamlit application. Keep it as one file until complexity demands splitting.

### Sections within app.py (in order)

1. Module docstring
2. Imports (stdlib → third-party)
3. Constants (`BACKEND_URL`, endpoints, `GENRE_OPTIONS`, `PAGE_SIZE`)
4. Logging setup (JSON format to `/var/log/app/frontend.log`)
5. API helper functions
6. `st.set_page_config(...)` — **must be the first Streamlit call**
7. Session state initialization
8. UI rendering

## Session State

All session state keys use `st_` prefix:

| Key | Type | Purpose |
|-----|------|---------|
| `st_search_query` | str | Current search text |
| `st_genre_filter` | str | Selected genre enum value |
| `st_current_page` | int | Current page (0-indexed) |

**Rule**: Always initialize session state keys with defaults before reading them.

## API Helper Functions

All functions that call the backend:
- Accept typed parameters
- Return the unwrapped `data` field from `ApiResponse`, or `None` on error
- Call `st.error()` (not `raise`) on failure so the UI degrades gracefully
- Log the request and response at INFO level

```python
def search_books(query: str, genre: str, page: int, size: int) -> Optional[dict]:
    """Returns unwrapped 'data' dict or None on error."""
    try:
        response = requests.get(API_SEARCH_ENDPOINT, params=..., timeout=10)
        response.raise_for_status()
        return response.json().get("data")
    except requests.exceptions.ConnectionError:
        st.error("Cannot connect to backend...")
        return None
```

## UI Conventions

- Use `st.container(border=True)` for book cards
- Use `st.columns()` for grid layout (2 columns for book results)
- Forms (`st.form`) for search — prevents re-run on every keystroke
- Pagination via buttons that update `st_current_page` and call `st.rerun()`
- All user-visible error messages are actionable (tell user what to do)

## Logging

```python
import logging
import os

LOG_DIR = os.environ.get("LOG_DIR", "/var/log/app")
os.makedirs(LOG_DIR, exist_ok=True)

logging.basicConfig(
    level=logging.INFO,
    format='{"timestamp": "%(asctime)s", "level": "%(levelname)s", "message": "%(message)s"}',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler(os.path.join(LOG_DIR, "frontend.log")),
    ],
)
logger = logging.getLogger(__name__)
```

## Running Locally (Without Docker)

```bash
cd frontend
pip install -r requirements.txt
BACKEND_URL=http://localhost:8080 streamlit run app.py --server.port 8501
```

## Adding a New Page

When the frontend grows, add pages in `frontend/pages/` using Streamlit's multi-page convention:
```
frontend/
├── app.py          # Main page
└── pages/
    ├── 1_book_detail.py
    └── 2_my_loans.py
```

Page files must start with a number for ordering.
