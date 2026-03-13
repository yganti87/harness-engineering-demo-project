# Design Doc 002: Session Strategy for User Authentication (F003)

**Date**: 2026-03-12
**Status**: Accepted
**Context**: F003 User Authentication — need a strategy for persisting login state across requests

## Decision

**Chosen: Option 2 — JWT (stateless)**

- Login returns a signed JWT containing `userId`, `username`, `exp` (expiry)
- Client sends `Authorization: Bearer <jwt>` on subsequent requests
- No server-side session storage; validation = verify signature + expiry
- Logout = client discards token (soft logout; token valid until expiry)
- Expiry: 15–60 min per SECURITY.md
- Client delivery: `Authorization` header (Streamlit stores token in session state)

See Option 2 details below.

---

## Problem

After login, the client must prove identity on subsequent requests. We need to:

1. Return a credential (token or session id) to the client on successful login
2. Validate that credential on future requests
3. Support logout (invalidate the credential)
4. Integrate with Streamlit (cookie-aware) and any future API clients

## Options Compared

| Option | Complexity | Scalability | Logout | Client Storage | Best For |
|--------|------------|-------------|--------|----------------|----------|
| **1. In-memory Map** | Low | Single instance only | Simple (remove from map) | Header or cookie | Local dev, single backend |
| **2. JWT (stateless)** | Medium | Horizontal scale | Weak (client discard only) | Header or cookie | Multi-instance, stateless APIs |
| **3. Spring Session (DB)** | Medium–High | Horizontal scale | Server-side invalidate | Cookie (HttpOnly) | Production, secure cookies |
| **4. JWT + Refresh Token (DB)** | High | Horizontal scale | Revoke refresh token | Header + secure cookie | Long-lived sessions, mobile/SPA |

---

## Option 1: In-Memory Session Store

**How it works**: On login, generate a random UUID, store `token → userId` in a `ConcurrentHashMap`. Return token in `LoginResponse`. Client sends `Authorization: Bearer <token>` or cookie. A filter/interceptor looks up token → userId.

**Pros**
- Minimal code: ~50 lines for store + filter
- No new dependencies
- Logout = remove from map
- Simple to test

**Cons**
- Lost on restart (all users must re-login)
- Single-instance only — no horizontal scaling
- Tokens never expire unless you add TTL logic

**Implementation**
- `SessionStore` bean (interface + in-memory impl)
- Custom `Filter` or `HandlerInterceptor` that checks `Authorization` header, resolves userId
- `AuthController` calls store on login; `logout` removes from store

**Fit for this project**: Good for Phase 1 if you expect a single backend instance and accept re-login on deploy. Fast to implement.

---

## Option 2: JWT (Stateless)

**How it works**: On login, sign a JWT containing `userId`, `username`, `exp` (expiry). Return JWT in `LoginResponse`. Client sends `Authorization: Bearer <jwt>`. No server-side storage; validation = verify signature + expiry.

**Pros**
- Stateless — scales to any number of backend instances
- No DB or cache for sessions
- Client can decode (read-only) for display (e.g. username)
- Standard approach for REST APIs

**Cons**
- Logout is soft only: token valid until expiry (can’t revoke without blacklist)
- Secret must be shared across instances
- Token size larger than opaque token

**Implementation**
- Add `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (or Spring Security OAuth2 Resource Server)
- `JwtService`: `generate(userId, username)`, `validate(token) → UserDto`
- Custom filter validates JWT, sets `SecurityContext` or request attribute
- Expiry: 15–60 min (per SECURITY.md guidance)

**Fit for this project**: Good if you want stateless auth and accept that logout = client discards token (token still valid until expiry). Aligns with SECURITY.md (“short expiry”).

---

## Option 3: Spring Session with Database

**How it works**: Use Spring Session JDBC. On login, Spring Security creates a server-side session (stored in `spring_session` table). Session id in HTTP-only cookie. No token in response body.

**Pros**
- Real logout: delete session row
- HTTP-only cookie = not accessible to JS (XSS protection)
- Horizontal scale (DB shared)
- Spring-native

**Cons**
- Requires Spring Security filter chain
- Cookie-based: CORS and same-site settings matter (Streamlit on different port/origin)
- Extra table + migration
- Heavier than Options 1 or 2

**Implementation**
- Add `spring-session-jdbc`, `spring-boot-starter-security`
- Configure `SecurityFilterChain`: formLogin or custom success handler that creates session
- `session_management` + JDBC session store
- Logout = `sessionRegistry.getSessionInformation(sessionId).expireNow()` or `SecurityContextLogoutHandler`

**Fit for this project**: Good if you want secure cookie-based auth and real logout. Streamlit must send cookies with requests (same-origin or proper CORS + credentials).

---

## Option 4: JWT + Refresh Token (DB)

**How it works**: Access token = short-lived JWT (15 min). Refresh token = long-lived opaque token stored in DB. Client stores both; uses refresh to get new access token. Logout = revoke refresh token in DB.

**Pros**
- Stateless access validation
- Real logout
- Long sessions without long-lived access tokens

**Cons**
- Most complex
- Requires refresh endpoint, token rotation logic
- Overkill for Phase 1

**Fit for this project**: Defer. Use when you add mobile/SPA with long sessions.

---

## Recommendation Summary

| If you need… | Choose |
|--------------|--------|
| Fastest Phase 1, single instance OK | **Option 1: In-memory** |
| Stateless, multi-instance ready, soft logout OK | **Option 2: JWT** |
| Real logout + HTTP-only cookie, production-ready | **Option 3: Spring Session DB** |

---

## Client Delivery Mechanism

Regardless of option:

- **Header**: `Authorization: Bearer <token>` — works for all options, simple for Streamlit (`requests` adds header)
- **Cookie**: Requires `credentials: 'include'` and backend `Set-Cookie` — better for XSS, more setup for cross-origin (Streamlit on 8501, backend on 8080)

For this project, **header** is simpler: Streamlit stores token in session state, adds header to API calls. Cookie is preferred only if you choose Spring Session (Option 3).

---

## Alternatives Considered

| Alternative | Rejected Because |
|-------------|------------------|
| In-memory Map | Single-instance only, lost on restart |
| Spring Session DB | Heavier; cookie/CORS setup more complex for Streamlit |
| JWT + Refresh Token | Overkill for Phase 1; defer for mobile/SPA |
