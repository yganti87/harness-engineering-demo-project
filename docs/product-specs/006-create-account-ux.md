# Product Spec 006: Create Account Success UX

**Feature ID**: F006
**Status**: planned

## User Story

As a new user, I want to see a clear confirmation screen after I successfully create an account so that I know my
registration worked and I understand how to proceed to log in.

## Problem

After a user successfully submits the Create Account form, the form fields remain populated with their entered values
and the only feedback is a small `st.success` banner below the form. This is poor UX: the form looks "stuck," there
is no clear call-to-action for the next step, and the user must manually switch to the Log In tab.

## Solution

Replace the registration form with a success confirmation view after successful account creation. The confirmation
view shows a clear success heading, a subtitle, and a "Go to Log In" button that navigates the user to the Log In tab.

## UX Design

### Create Account Tab — Before Submission (unchanged)

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

### Create Account Tab — After Successful Registration (new)

```
│          ┌──────────────────────────────────────────┐               │
│          │  [  Log In  ]  |  [ Create Account ]     │               │
│          ├──────────────────────────────────────────┤               │
│          │                                          │               │
│          │           Account Created!               │               │
│          │  Your account has been created           │               │
│          │  successfully.                           │               │
│          │                                          │               │
│          │  [       Go to Log In        ]           │               │
│          │                                          │               │
│          └──────────────────────────────────────────┘               │
```

### Create Account Tab — After Failed Registration (unchanged)

Form remains visible. Error messages from `register_user()` are displayed via `st.error()` within the form,
allowing the user to correct input and resubmit.

## API Contract

No new API endpoints. This feature uses the existing registration endpoint:

- `POST /api/v1/auth/register` — registration

## Implementation Notes

### State Management

- Add `st.session_state.st_registration_success` (bool, default `False`) to track post-registration state.
- On successful `register_user()` call: set flag to `True`, call `st.rerun()`.
- In the Create Account tab render block: branch on the flag.
  - If `True`: render success view (heading, subtitle, "Go to Log In" button).
  - If `False`: render the existing registration form.
- "Go to Log In" button: clear flag, call `st.rerun()`.

### Files to Modify

| File | Change |
|------|--------|
| `frontend/app.py` | Add `st_registration_success` session state flag; conditionally render success view or form |

### Files NOT to Modify

- No backend changes
- No database migrations
- `register_user()` function — error handling already works; no changes needed

## Acceptance Criteria

See `features.json` entry F006.

## Out of Scope

- Auto-login after registration
- Email verification
- Password strength indicator
- Animated transitions
- Backend changes
