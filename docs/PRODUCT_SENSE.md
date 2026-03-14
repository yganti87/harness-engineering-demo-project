# Product Sense

## Vision

A **library catalog** that starts simple (anonymous search) and grows into a full library management system with user accounts, borrowing, recommendations, and admin tools — all built by coding agents without manual code.

The primary constraint: every feature must be testable, observable, and maintainable by an agent with access only to this repository and its documentation.

## Current State

Anonymous book search is live. Users can:
- Search by title, author, or ISBN
- Filter by genre
- Browse paginated results

## Feature Roadmap

| ID | Feature | Priority | Status |
|----|---------|----------|--------|
| F001 | Anonymous Book Search | High | completed |
| F002 | Book Detail View | Medium | planned |
| F003 | User Registration & Login | Medium | completed |
| F004 | Prometheus & Grafana Observability | Medium | completed |
| F005 | User Borrowing (check out / return) | Medium | planned |
| F006 | Book Availability Status | Medium | planned |
| F007 | User Reading History | Low | planned |
| F008 | Book Recommendations | Low | planned |
| F009 | Admin: Add/Edit/Delete Books | Medium | planned |
| F010 | Admin: Manage Users | Low | planned |
| F011 | Export Catalog (CSV/PDF) | Low | planned |

## Design Principles

1. **Anonymous first**: All catalog browsing works without login
2. **Search is core**: Fast, relevant search is the most important UX feature
3. **Simple UI**: Streamlit is a tool for rapid iteration, not pixel-perfect design
4. **Observable**: Every feature emits structured logs and is testable via API
5. **Harness first**: Before implementing a feature, ensure the harness can support it (tests, docs, constraints)

## Non-Goals

- Mobile-native app (web-first)
- Real-time collaboration
- eBook reading/viewing
- Payment processing
