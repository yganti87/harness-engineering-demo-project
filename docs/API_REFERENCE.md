# API Reference

Base URL: `http://localhost:8080`

All responses are wrapped in `ApiResponse<T>`:
```json
{
  "status": 200,
  "data": { ... },
  "error": null,
  "timestamp": "2026-03-12T10:00:00Z"
}
```

Interactive docs: http://localhost:8080/swagger-ui.html

---

## Books

### Search Books

`GET /api/v1/books/search`

Search and filter books with pagination.

**Query Parameters**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `q` | string | No | — | Search term (matches title, author, or ISBN, case-insensitive) |
| `genre` | string | No | — | Filter by genre enum value (see Genre enum below) |
| `page` | integer | No | 0 | Page number (0-indexed) |
| `size` | integer | No | 20 | Page size (1–100) |

**Example Request**
```bash
curl "http://localhost:8080/api/v1/books/search?q=spring&genre=TECHNOLOGY&page=0&size=10"
```

**Success Response** (200)
```json
{
  "status": 200,
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Spring in Action",
        "author": "Craig Walls",
        "isbn": "9781617294945",
        "genre": "TECHNOLOGY",
        "publicationYear": 2022,
        "description": "Spring in Action guides you through the Spring framework..."
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "error": null,
  "timestamp": "2026-03-12T10:00:00Z"
}
```

**Empty Result** (200)
```json
{
  "status": 200,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  },
  "error": null,
  "timestamp": "2026-03-12T10:00:00Z"
}
```

**Validation Error** (400)
```json
{
  "status": 400,
  "data": null,
  "error": "size: must be between 1 and 100",
  "timestamp": "2026-03-12T10:00:00Z"
}
```

---

### Get Book by ID

`GET /api/v1/books/{id}`

Get full details of a single book.

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Book identifier |

**Example Request**
```bash
curl "http://localhost:8080/api/v1/books/550e8400-e29b-41d4-a716-446655440000"
```

**Success Response** (200)
```json
{
  "status": 200,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Spring in Action",
    "author": "Craig Walls",
    "isbn": "9781617294945",
    "genre": "TECHNOLOGY",
    "publicationYear": 2022,
    "description": "Spring in Action guides you through..."
  },
  "error": null,
  "timestamp": "2026-03-12T10:00:00Z"
}
```

**Not Found** (404)
```json
{
  "status": 404,
  "data": null,
  "error": "Book not found: id=550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-12T10:00:00Z"
}
```

---

## System Endpoints

### Health Check

`GET /actuator/health`

```bash
curl http://localhost:8080/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Application Logs

`GET /actuator/logfile`

```bash
curl http://localhost:8080/actuator/logfile | tail -50
```

### Flyway Migration Status

`GET /actuator/flyway`

```bash
curl http://localhost:8080/actuator/flyway
```

---

## Genre Enum Values

| Value | Label |
|-------|-------|
| `FICTION` | Fiction |
| `NON_FICTION` | Non-Fiction |
| `TECHNOLOGY` | Technology |
| `SCIENCE` | Science |
| `HISTORY` | History |
| `BIOGRAPHY` | Biography |
| `MYSTERY` | Mystery |
| `ROMANCE` | Romance |
| `FANTASY` | Fantasy |
| `SCIENCE_FICTION` | Science Fiction |
| `SELF_HELP` | Self-Help |
| `BUSINESS` | Business |
| `OTHER` | Other |
