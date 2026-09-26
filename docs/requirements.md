# ACME Compensation Hub — Product Requirements

**Version:** 1.0 · **Date:** September 26, 2026

---

## Goal

Give ACME's HR team a single, reliable place to manage compensation data for up to 10,000 employees and answer organizational pay questions — replacing the error-prone spreadsheets that currently serve as the source of truth.

---

## Persona

**HR Manager.** Manages employee records, sets and updates salaries, and reports pay statistics to leadership. Works across departments and geographies. Does not process payroll or administer benefits — those live in separate systems.

---

## Problem

Compensation data is currently scattered across spreadsheets. There is no audit trail for salary changes, no consistent way to answer "what is the average salary in Engineering across our APAC offices?", and no guardrails to prevent bad data from creeping in. As the company approaches 10,000 employees across multiple countries, this is becoming a real operational risk.

---

## MVP Scope

Three areas, nothing more.

**Employee directory.** Create, update, and deactivate employee records. Each record captures name, email, department, job title, country, and status. Deactivation preserves history rather than deleting data.

**Compensation management.** Record and update each employee's salary and currency. Every change is stored as an immutable history entry with an effective date and the identity of who made the change. The current salary is always the most recent entry with an effective date on or before today — there is no overwrite, only append.

**Compensation analytics.** Answer real HR questions: headcount, average salary, median salary, salary range, broken down by department or country. Analytics exclude inactive employees by default.

---

## Non-Functional Requirements

- List and detail API responses under 200 ms (p95) on the full 10,000-employee dataset.
- Analytics queries under 1 second (p95).
- Salary values never appear in logs or error responses.
- All inputs validated; all SQL parameterized.
- Database enforces referential integrity and salary history uniqueness (one record per employee per effective date).
- Application runs locally with a single `docker-compose up`.

---

## Out of Scope

**Payroll processing and tax calculations** are regulated, jurisdiction-specific domains with their own compliance requirements. They belong in dedicated systems.

**Benefits administration** has a separate data model and ownership; conflating it with compensation would bloat this tool without serving the HR Manager's core workflow.

**Employee self-service** — the sole persona here is the HR Manager. Individual contributors viewing or requesting changes to their own compensation is a different product with different trust and approval requirements.

**Multi-step approval workflows** are not required by the assessment and would add significant complexity. A single HR Manager making changes with a full audit trail is sufficient for now.

**Real-time infrastructure, microservices, and container orchestration** are not justified at this scale. A well-indexed PostgreSQL database behind a Spring Boot monolith handles 10,000 employees without any of that overhead.

---

## Tech Stack

Java / Spring Boot · Angular · PostgreSQL · Liquibase

Business logic in a service layer, tested independently of the database. Repository integration tests run against real PostgreSQL via Testcontainers. Liquibase changelogs versioned alongside the code and applied automatically on startup.

---

*Salary is sensitive data. It is excluded from list endpoints by default and must never appear in logs.*
