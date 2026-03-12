# Product Spec 001: Book Search

**Feature ID**: F001
**Status**: completed

## User Story

As a library visitor, I want to search for books by title, author, or topic so that I can find books to borrow without needing to create an account.

## UX Design

```
┌─────────────────────────────────────────────────────────────────┐
│ Library Catalog                                                  │
│ Search our collection. No account required.                      │
├──────────────────────────────────┬──────────────────┬──────────┤
│ [Search by title, author, ISBN…] │ [All Genres ▾]   │ [Search] │
├─────────────────────────────────────────────────────────────────┤
│ Found 3 books matching 'spring' in Technology                    │
├─────────────────────────────┬───────────────────────────────────┤
│ ┌─────────────────────────┐ │ ┌─────────────────────────────┐  │
│ │ Spring in Action         │ │ │ Spring Boot in Practice     │  │
│ │ Craig Walls              │ │ │ Mark Heckler                │  │
│ │ Technology | 2022        │ │ │ Technology | 2022           │  │
│ │ ISBN: 978-1617294945     │ │ │ ISBN: 978-1617298813        │  │
│ │ Spring in Action covers… │ │ │ Hands-on guide to Spring…   │  │
│ └─────────────────────────┘ │ └─────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┤
│              ← Previous   Page 1 of 1   Next →                   │
└─────────────────────────────────────────────────────────────────┘
```

## API Contract

`GET /api/v1/books/search?q={query}&genre={genre}&page={page}&size={size}`

See [API_REFERENCE.md](../API_REFERENCE.md) for full spec.

## Acceptance Criteria

See `features.json` entry F001.

## Out of Scope

- Saving search history
- Book availability status (F005)
- User authentication (F003)
