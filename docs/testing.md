# ACME Compensation Hub — Testing Strategy & Coverage

**Version:** 1.0 · **Date:** September 27, 2026

---

## Overview

The test suite covers **backend unit tests**, **backend API slice tests**, and **frontend component + service tests**. Integration tests (Testcontainers against a real PostgreSQL) exist in the codebase and run in a separate CI stage to avoid slowing fast feedback.

---

## Backend Test Suite

### Tools

| Tool | Purpose |
|---|---|
| JUnit 5 | Test runner and assertions |
| Mockito | Repository and dependency mocking for unit tests |
| Spring Boot Test / MockMvc | Full HTTP stack slice tests (mocked service layer) |
| Testcontainers | Real PostgreSQL for repository integration tests |
| JaCoCo | Coverage measurement and enforcement gate |

### Coverage Gate (enforced in CI)

```
Line coverage:   >= 70%
Branch coverage: >= 60%
```

Configured in `backend/pom.xml` under the `jacoco-maven-plugin` execution. The CI `test` job fails if either threshold is not met.

### Test File Map

| Test Class | Type | What is covered |
|---|---|---|
| `EmployeeServiceTest` | Unit | Create, update, deactivate, duplicate email, not-found, status logic |
| `EmployeeControllerTest` | API slice | All REST endpoints: happy path, 400 validation, 404, 409 conflict |
| `EmployeeTest` | Unit | JPA entity state and field constraints |
| `EmployeeStatusConverterTest` | Unit | `ACTIVE`/`INACTIVE` enum ↔ DB string conversion, invalid values |
| `CompensationServiceTest` | Unit | Record salary, current salary resolution, history, employee not found |
| `CompensationControllerTest` | API slice | `POST /compensation`, `GET /compensation`, `GET /compensation/history` |
| `CompensationHistoryTest` | Unit | Entity constraints and field-level checks |
| `AnalyticsServiceTest` | Unit | `mapRow` projection, null handling, multi-currency grouping |
| `AnalyticsControllerTest` | API slice | `GET /analytics/departments`, `GET /analytics/summary`, filter params |
| `ApiResponseTest` | Unit | Envelope builder: success, error, pagination meta |
| `GlobalExceptionHandlerTest` | Unit | 400, 404, 409, 500 error shapes and status codes |
| `CompensationApplicationTests` | Spring context | Context loads without errors |
| `TestFixtures` | Shared factory | Canonical `Employee` and `CompensationHistory` builders for all tests |

### What Is NOT Unit-Tested (and why)

| Area | Reason |
|---|---|
| `AnalyticsRepository` native SQL | SQL correctness can only be validated against a real PostgreSQL engine (PERCENTILE_CONT, DISTINCT ON). Covered by Testcontainers integration tests. |
| `EmployeeRepository` JPA queries | Standard Spring Data query derivation. Integration-tested with Testcontainers. |
| Liquibase changelogs | Applied and validated in Testcontainers integration tests; cannot be meaningfully mocked. |

---

## Frontend Test Suite

### Tools

| Tool | Purpose |
|---|---|
| Karma + Jasmine | Test runner and assertion framework |
| Angular TestBed | Component setup and dependency injection |
| jasmine.createSpyObj | Service mocking (HttpClient, Router, MatDialog, MatSnackBar) |

### Total: **131 tests, 131 passing**

Run with:
```bash
npm test -- --watch=false --browsers=ChromeHeadless
```

### Test File Map

| Spec File | Tests | What is covered |
|---|---|---|
| `app.component.spec.ts` | 2 | App root mounts; router outlet present |
| `api.service.spec.ts` | 8 | GET/POST/PUT/PATCH wrappers; error propagation |
| `employee.service.spec.ts` | 9 | list, get, create, update, deactivate; maps API envelope correctly |
| `compensation.service.spec.ts` | 6 | getCurrent, record, getHistory; error forwarding |
| `analytics.service.spec.ts` | 5 | getDepartmentStats, getSummary; country param threading |
| `theme.service.spec.ts` | 8 | ThemeService: light/dark/system; localStorage persistence; class/attr applied to both `html` and `body` |
| `employee-list.component.spec.ts` | 22 | Init load, error handling, search debounce, pagination, create/edit dialog open/close, deactivate flow, empty state, loading state |
| `employee-detail.component.spec.ts` | 14 | Load employee + compensation, tab switching, dialog open/close, not-found redirect |
| `employee-form-dialog.component.spec.ts` | 10 | Create mode, edit mode, validation, cancel, submit success/error |
| `analytics-dashboard.component.spec.ts` | 18 | Country dropdown init, country filter, 'All Countries' breakdown, currency symbol rendering, multi-currency safeguard, table columns |
| `compensation-form-dialog.component.spec.ts` | 8 | Form validation, submit, cancel, error snackbar |
| `shell.component.spec.ts` | 9 | Sidenav toggle, active route highlighting, theme menu open/close, theme selection |
| `confirm-dialog.component.spec.ts` | 4 | Renders message, confirm/cancel button actions |
| `currency-display.component.spec.ts` | 7 | All 8 currency symbols ($, £, €, ₹, CA$, A$, S$, default), amount formatting |
| `empty-state.component.spec.ts` | 4 | Renders icon, message, optional action button |
| `page-header.component.spec.ts` | 3 | Renders title, subtitle, optional action slot |
| `status-badge.component.spec.ts` | 4 | ACTIVE → green, INACTIVE → grey, CSS class binding |

### Scenarios Intentionally Not Covered by Frontend Tests

| Scenario | Reason |
|---|---|
| Nginx proxy routing (`/api/` → backend) | Integration concern; requires a running container stack |
| Angular routing guards (future auth) | No guards implemented in MVP |
| Browser-specific rendering differences | E2E / visual regression testing (stretch goal) |

---

## Test Pyramid Summary

```
              [ E2E / Cypress ]           ← stretch goal, not implemented
            ──────────────────────
         [ Frontend: 131 component tests ]   ← all passing
      ────────────────────────────────────────
   [ Backend: ~60 unit + API slice tests ]    ← all passing, coverage gate met
──────────────────────────────────────────────────────
[ Testcontainers integration tests (real PostgreSQL) ]  ← tagged @Tag("integration")
```

The `integration` group is excluded from fast CI runs (`-DexcludedGroups=integration`) and runs separately on demand or in a dedicated slow-test stage.

---

## Seed Data for Tests

Backend integration tests use `TestFixtures.java` to build canonical entity instances. Frontend tests use inline fixture constants (`ALICE`, `META`) scoped to each spec file. No shared mutable state between test runs.

The Liquibase seed changeset (`0004-seed-data.xml`) runs automatically in `test` and `local` Liquibase contexts, providing 10,000 deterministic employees for repository and analytics integration tests.
