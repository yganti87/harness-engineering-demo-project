# Product Spec 005: Auth-Gated Library Catalog with Redesigned Landing Page

**Feature ID**: F005
**Status**: planned

## User Story

As an unauthenticated visitor, I want to see a clear and inviting landing page with prominent login and
registration options so that I understand the value of creating an account and can easily sign in or sign up
without hunting through collapsed menus.

## Problem

The current UI has two usability issues:

1. **Hidden auth entry points** — Login and Create Account are tucked inside small collapsed expanders in the
   top-right corner, making them easy to miss and awkward to use.
2. **No incentive to authenticate** — The full book catalog is visible to unauthenticated users, so there is no
   perceived benefit to creating an account.

## Solution

Gate the catalog behind authentication. Unauthenticated users see only a landing page with a hero section and
prominent tabs for Log In and Create Account. The full search and browse experience is only shown after login.

## UX Design

### Logged-Out State (Landing Page)

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│                      📚 Library Catalog                              │
│                                                                     │
│   Discover, search, and explore our curated collection of books.    │
│   Sign in to browse the full catalog, filter by genre, and find    │
│   your next great read.                                             │
│                                                                     │
│          ┌──────────────────────────────────────────┐               │
│          │  [  Log In  ]  |  [ Create Account ]     │               │
│          ├──────────────────────────────────────────┤               │
│          │  Welcome back                            │               │
│          │                                          │               │
│          │  Username: [________________________]    │               │
│          │  Password: [________________________]    │               │
│          │                                          │               │
│          │  [         Log In         ]              │               │
│          └──────────────────────────────────────────┘               │
│                                                                     │
│              Free to use. No credit card required.                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Create Account Tab (still logged-out)

```
│          ┌──────────────────────────────────────────┐               │
│          │  [  Log In  ]  |  [ Create Account ]     │               │
│          ├──────────────────────────────────────────┤               │
│          │  Create your account                     │               │
│          │                                          │               │
│          │  Username: [________________________]    │               │
│          │  Password: [________________________]    │               │
│          │  Confirm Password: [________________]    │               │
│          │                                          │               │
│          │  [      Create Account       ]           │               │
│          └──────────────────────────────────────────┘               │
```

### Logged-In State (Catalog Experience — unchanged)

```
┌─────────────────────────────────────┬───────────────────────────────┐
│ 📚 Library Catalog                  │  Welcome, alice   [Logout]    │
├──────────────────────┬──────────┬───┴───────────────────────────────┤
│ [Search by title…]   │[Genre ▾] │ [Search]                          │
├─────────────────────────────────────────────────────────────────────┤
│ Showing all 10 books in the catalog                                  │
│  ┌──────────────────┐   ┌──────────────────┐                        │
│  │  Book Title       │   │  Book Title       │                       │
│  │  Author           │   │  Author           │                       │
│  └──────────────────┘   └──────────────────┘                        │
│               ← Previous   Page 1 of 1   Next →                     │
└─────────────────────────────────────────────────────────────────────┘
```

## API Contract

No new API endpoints are required. This feature uses existing endpoints:

- `POST /api/v1/auth/login` — login
- `POST /api/v1/auth/register` — registration
- `GET /api/v1/books/search` — book search (used only in logged-in state)

See [API_REFERENCE.md](../API_REFERENCE.md) for full spec.

## Implementation Notes

### Frontend Structure (`frontend/app.py`)

The main rendering block gates on `st.session_state.st_user`:

```
if st.session_state.st_user is None:
    render_landing_page()   # hero + tabs
else:
    render_catalog()        # header + search + results (unchanged)
```

**`render_landing_page()`**:
- Centered title and subtitle using `st.title` / `st.markdown`
- `[1, 2, 1]` column layout to center the forms
- `st.tabs(["Log In", "Create Account"])` inside the center column
- Log In tab: "Welcome back" heading, Username, Password, full-width Log In button
- Create Account tab: "Create your account" heading, Username (max 50 chars), Password, Confirm Password,
  full-width Create Account button
- Footer: `st.caption("Free to use. No credit card required.")`

**`render_catalog()`**:
- Header row: title on left, "Welcome, {username}" + Logout button on right
- Search form and paginated book grid (logically identical to current implementation)

**Unchanged**:
- All API helper functions: `search_books`, `register_user`, `login_user`
- `render_book_card` and `render_pagination`
- All session state keys (`st_` prefix convention)
- `st.set_page_config` call (must remain first Streamlit call)

**On successful registration**: show `st.success` directing user to switch to the Log In tab (do NOT auto-rerun
into the catalog; user must explicitly log in).

**On successful login**: set `st.session_state.st_user` and `st.session_state.st_token`, then call
`st.rerun()` to enter the catalog.

**On logout**: clear `st.session_state.st_user` and `st.session_state.st_token`, then `st.rerun()` to return
to the landing page.

## Acceptance Criteria

See `features.json` entry F005.

## Files to Modify

| File | Change |
|------|--------|
| `frontend/app.py` | Restructure UI: add `render_landing_page()` and `render_catalog()`, gate on `st_user` |

## Files NOT to Modify

- No backend changes (no new endpoints, no migrations)
- No database changes
- API helper functions remain functionally identical (signatures, return types, error handling)

## Out of Scope

- Backend authorization — the API continues to serve unauthenticated requests; gating is frontend-only
- "Remember me" / persistent sessions
- Password reset or email verification
- Social login (OAuth)
- Book detail view (F002)
