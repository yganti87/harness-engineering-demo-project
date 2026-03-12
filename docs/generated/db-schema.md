# Database Schema

> Auto-generated from `backend/src/main/resources/db/migration/`.
> Update this file when adding new migrations.

## Migration History

| Version | Description | File |
|---------|-------------|------|
| V1 | Create library schema + seed data | `V1__create_library_schema.sql` |

## Tables

### `books`

The primary catalog table.

| Column | Type | Nullable | Constraints | Description |
|--------|------|----------|-------------|-------------|
| `id` | UUID | No | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique identifier |
| `title` | VARCHAR(500) | No | NOT NULL | Book title |
| `author` | VARCHAR(300) | No | NOT NULL | Author name(s) |
| `isbn` | VARCHAR(20) | Yes | UNIQUE | ISBN-10 or ISBN-13 |
| `genre` | VARCHAR(50) | Yes | CHECK genre_values | Genre enum value |
| `publication_year` | INTEGER | Yes | | Year published |
| `description` | TEXT | Yes | | Book description |
| `created_at` | TIMESTAMPTZ | No | DEFAULT NOW() | Record creation time |

**Indexes**:
- Primary: `books_pkey` on `id`
- Unique: `books_isbn_key` on `isbn`
- Search: `idx_books_title_author` on `(title, author)` for LIKE queries
- Genre: `idx_books_genre` on `genre`

**Genre check constraint** values:
`FICTION, NON_FICTION, TECHNOLOGY, SCIENCE, HISTORY, BIOGRAPHY, MYSTERY, ROMANCE, FANTASY, SCIENCE_FICTION, SELF_HELP, BUSINESS, OTHER`

## Seed Data

V1 migration inserts 10 sample books for development and integration testing.
Integration tests assert against these known books.
