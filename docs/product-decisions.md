# ACME Compensation Hub — Product Decisions

**Version:** 1.0 · **Date:** September 26, 2026

This document records the significant design decisions made during requirements and architecture, the alternatives considered, and the reasoning behind each choice. It exists so that future contributors understand not just what was built, but why — and can revisit a decision if the context changes.

---

## 1. Monolith over microservices

**Decision:** Single Spring Boot application, single PostgreSQL database.

**Why:** The system serves one persona (HR Manager), one domain (compensation), and a bounded dataset (10,000 employees). Microservices would distribute that into separate deployable units with inter-service communication, independent deployments, and distributed tracing — none of which provide value at this scale. A monolith is simpler to build, test, and reason about. If the team grows or the domain expands significantly, the package-by-feature structure makes extraction straightforward later.

---

## 2. Append-only compensation history

**Decision:** Salary changes are never updated or deleted. Every change creates a new `compensation_history` row with an `effective_date`. Current salary is derived by querying for the most recent row with `effective_date <= today`.

**Why:** Compensation data is sensitive and legally significant. HR Managers and auditors need to know what someone was paid at any point in time, not just today. An update-in-place model loses that. The append-only model gives you a full audit trail for free, without a separate audit table or trigger-based logging. The tradeoff — slightly more complex current-salary queries — is minor and well-understood.

---

## 3. Salary excluded from list endpoints by default

**Decision:** The employee list API does not return salary fields. Salary is only returned when explicitly requesting a compensation detail or history endpoint.

**Why:** Salary is sensitive. Returning it in a list of 10,000 employees means it travels over the wire, appears in browser network tabs, and risks exposure in logs or error traces even when no one asked for it. Requiring an explicit request creates a meaningful access boundary. This is enforced at the API level, not left to frontend discretion.

---

## 4. Native SQL for analytics, JPA for CRUD

**Decision:** Standard CRUD operations use Spring Data JPA. Analytics queries (median, percentile, aggregations by department/country) are written as native SQL with `@Query(nativeQuery = true)`.

**Why:** JPA is productive for simple queries and keeps the codebase consistent. But JPQL cannot express `PERCENTILE_CONT`, window functions, or certain aggregations cleanly. Forcing analytics through JPQL would produce either unreadable HQL or load all rows into memory and compute in Java — both worse than native SQL. The boundary is clear: if JPA can express it readably, use JPA. If it cannot, use SQL.

---

## 5. Testcontainers over H2 for repository tests

**Decision:** Integration tests spin up a real PostgreSQL instance via Testcontainers rather than using an H2 in-memory database.

**Why:** H2's PostgreSQL compatibility mode does not support `PERCENTILE_CONT` or several other PostgreSQL-specific constructs used in analytics queries. Tests that pass against H2 can fail against a real database. Testcontainers adds a few seconds to the test run but tests against the actual engine, which is the only definition of "passing" that matters.

---

## 6. Liquibase for schema management

**Decision:** All DDL is managed through Liquibase changelogs. No schema changes are made by manually running SQL against any environment.

**Why:** Without migration tooling, schema drift between environments is inevitable. Liquibase provides versioned, ordered, and auditable changelogs that run automatically on startup. Rollback is supported if needed. The seed data changeset is gated by a Liquibase context so it only runs in `local` and `test` profiles — it cannot accidentally populate a production database.

---

## 7. No currency conversion

**Decision:** Salaries are stored in their native currency. The system does not convert between currencies.

**Why:** Accurate currency conversion requires a live exchange rate feed and a decision about which rate to use (spot, monthly average, fiscal year). That is a non-trivial data dependency. The requirement is to manage and analyze compensation data — not to produce a single comparable number across all countries. HR Managers can filter analytics by currency or country when cross-border comparison matters. Currency conversion can be added if explicitly required.

---

## 8. Departments and countries as free-text strings

**Decision:** Department and country are stored as plain strings, not foreign keys to reference tables.

**Why:** Reference tables add correctness but require upfront definition, migration scripts, and UI management. For an MVP, the cost outweighs the benefit. Normalizing to uppercase on write and validating ISO 3166-1 alpha-2 for country codes at the API layer provides enough consistency. If the business needs canonical department taxonomies later, migrating to a reference table is a straightforward operation.

---

## 9. No authentication implementation in MVP

**Decision:** The system assumes an authenticated HR Manager. A simple bearer token stub is acceptable for the assessment. Full auth is out of scope.

**Why:** Implementing authentication properly (OAuth2, session management, token refresh, secure storage) is a significant surface area. The assessment is evaluating compensation domain logic, API design, and data modeling — not auth infrastructure. A stub keeps the scope honest without leaving a security hole in a production deployment.

---

## 10. No real-time updates

**Decision:** The UI is request-response. There are no WebSockets, SSE, or push notifications.

**Why:** Compensation data does not change frequently enough to justify real-time infrastructure. An HR Manager updating a salary record does not need the change pushed to other open browser tabs. Polling or manual refresh is sufficient, and neither adds complexity to the system.
