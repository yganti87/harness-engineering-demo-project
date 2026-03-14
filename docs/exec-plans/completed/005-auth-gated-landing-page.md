# Execution Plan 005: Auth-Gated Landing Page (F005)

**Feature**: F005 — Auth-Gated Landing Page
**Status**: completed
**Product Spec**: [005-auth-gated-landing-page.md](../product-specs/005-auth-gated-landing-page.md)
**Started**: 2026-03-14
**Completed**: 2026-03-14

## Goal

Restructure the Streamlit frontend so that unauthenticated users see a clean, centered landing page with tabbed Login/Register forms instead of the book catalog. Only logged-in users can browse, search, and filter books.

## Acceptance Criteria

From `features.json`: All criteria met.

1. Unauthenticated users see landing page only — no books, no search form
2. Landing page has centered hero with title + subtitle
3. Tabbed auth forms: Log In / Create Account
4. Successful login redirects to catalog
5. Successful registration shows success message (no auto-redirect)
6. Logged-in users see full catalog with welcome + logout in header
7. Logout returns to landing page

## Implementation Steps

### Step 1: Extract `render_landing_page()` function

- Centered hero section using `st.columns([1, 2, 1])` with HTML title and subtitle
- Tabbed auth forms (`st.tabs(["Log In", "Create Account"])`) in centered column
- Login tab: username/password form, on success set session state and `st.rerun()`
- Register tab: username/password/confirm form, on success show `st.success()` (no rerun)
- Footer caption: "Free to use. No credit card required."

### Step 2: Extract `render_catalog()` function

- Header row with title + welcome/logout (no if/else for auth state)
- Search form (unchanged from existing)
- Paginated results grid (unchanged from existing)

### Step 3: Add auth gate in main rendering block

```python
if st.session_state.st_user is None:
    render_landing_page()
else:
    render_catalog()
```

Replaced the old inline rendering section (lines 294–435) with the gate.

### Step 4: Update frontend tests

- Added `_call_register_api()` and `_call_login_api()` extracted logic helpers
- Added `TestRegisterApiHelper` (4 tests): valid registration, password mismatch, connection error, duplicate username (409)
- Added `TestLoginApiHelper` (3 tests): valid credentials, invalid credentials (401), connection error

### Step 5: Update `features.json`

- F005 status set to `completed`
- `testSteps` populated with all 11 test method names
- `implementedFiles` set to `["frontend/app.py", "frontend/tests/test_app.py"]`

## Files Modified

| File | Change |
|------|--------|
| `frontend/app.py` | Restructured into `render_landing_page()` + `render_catalog()` with auth gate |
| `frontend/tests/test_app.py` | Added 7 new tests for login/register API helpers |
| `features.json` | F005 marked completed |
| `docs/product-specs/005-auth-gated-landing-page.md` | Product spec added |

## Potential Pitfalls

1. **Form key collisions**: Landing page and catalog use different form keys; only one renders per rerun cycle, so no collision risk.
2. **`st.set_page_config` placement**: Function definitions placed before `set_page_config` but actual calls happen after — safe.
3. **No `st.rerun()` after registration**: By design, user must switch to Log In tab manually.

## Test Results

All 11 frontend tests pass (0 failures, 0 errors).
