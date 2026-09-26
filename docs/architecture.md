# ACME Compensation Hub — Architecture

**Version:** 1.0 · **Date:** September 26, 2026

---

## Overview

The system is a monolithic web application. One backend process, one database, one frontend application. This is the right shape for a single-persona, single-domain tool managing 10,000 employees. Distributed architecture would introduce operational complexity without providing any benefit at this scale.

---

## System Diagram

```
┌─────────────────────────────────────────────────────────┐
│                     Browser                             │
│                                                         │
│   Angular SPA                                           │
│   ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  │
│   │  Employee   │  │ Compensation │  │  Analytics   │  │
│   │  Module     │  │ Module       │  │  Module      │  │
│   └──────┬──────┘  └──────┬───────┘  └──────┬───────┘  │
│          └────────────────┴─────────────────┘           │
│                     HttpClient Service                  │
└────────────────────────┬────────────────────────────────┘
                         │  HTTPS · JSON · REST
┌────────────────────────▼────────────────────────────────┐
│              Spring Boot Application                    │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  REST Controllers  (@RestController)             │   │
│  │  Input validation  (Bean Validation / @Valid)    │   │
│  └────────────────────────┬─────────────────────────┘   │
│                           │                             │
│  ┌────────────────────────▼─────────────────────────┐   │
│  │  Service Layer  (@Service)                       │   │
│  │  Business logic · current-salary resolution      │   │
│  │  Analytics aggregation orchestration             │   │
│  └────────────────────────┬─────────────────────────┘   │
│                           │                             │
│  ┌────────────────────────▼─────────────────────────┐   │
│  │  Repository Layer  (Spring Data JPA)             │   │
│  │  Standard CRUD via JpaRepository                 │   │
│  │  Analytics via @Query native SQL                 │   │
│  └────────────────────────┬─────────────────────────┘   │
└────────────────────────────┬────────────────────────────┘
                             │  JDBC / Hibernate
┌────────────────────────────▼────────────────────────────┐
│                     PostgreSQL                          │
│                                                         │
│   employees · compensation_history                      │
│   Schema managed by Liquibase changelogs                │
└─────────────────────────────────────────────────────────┘
```

---

## Backend — Spring Boot

**Package structure**

```
com.acme.compensation
├── employee
│   ├── EmployeeController.java
│   ├── EmployeeService.java
│   ├── EmployeeRepository.java
│   ├── Employee.java              (JPA entity)
│   └── dto/
│       ├── EmployeeRequest.java
│       └── EmployeeResponse.java
├── compensation
│   ├── CompensationController.java
│   ├── CompensationService.java
│   ├── CompensationRepository.java
│   ├── CompensationHistory.java   (JPA entity)
│   └── dto/
│       ├── CompensationRequest.java
│       └── CompensationResponse.java
├── analytics
│   ├── AnalyticsController.java
│   ├── AnalyticsService.java
│   ├── AnalyticsRepository.java
│   └── dto/
│       └── DepartmentStats.java
└── common
    ├── ApiResponse.java           (standard envelope)
    ├── GlobalExceptionHandler.java
    └── PageResponse.java
```

**Layer responsibilities**

- *Controllers* handle HTTP concerns only: deserialize the request, call the service, serialize the response. No business logic.
- *Services* own all business logic: current-salary resolution, validation rules that span multiple entities, analytics orchestration. No JPA or SQL.
- *Repositories* own all data access. Standard CRUD via `JpaRepository`. Analytics queries written as native SQL using `@Query(nativeQuery = true)` — JPQL cannot express `PERCENTILE_CONT`.

**API conventions**

- Base path: `/api/v1`
- All responses wrapped in `{ "data": ..., "meta": ..., "errors": [...] }`
- Paginated lists include `{ "page", "pageSize", "totalElements", "totalPages" }` in `meta`
- Errors include field-level detail: `{ "field": "currency", "message": "must be a valid ISO 4217 code" }`
- HTTP status codes follow REST conventions: 200, 201, 400, 404, 409, 500

**Key endpoints**

```
GET    /api/v1/employees                  list with filter + pagination
POST   /api/v1/employees                  create
GET    /api/v1/employees/{id}             detail (no salary by default)
PUT    /api/v1/employees/{id}             update
PATCH  /api/v1/employees/{id}/deactivate  soft delete

GET    /api/v1/employees/{id}/compensation         current salary
POST   /api/v1/employees/{id}/compensation         record new salary
GET    /api/v1/employees/{id}/compensation/history full history

GET    /api/v1/analytics/departments       stats per department
GET    /api/v1/analytics/summary           org-wide summary
```

---

## Frontend — Angular

**Module structure**

Three feature modules map directly to the three MVP areas: `EmployeeModule`, `CompensationModule`, `AnalyticsModule`. A shared `CoreModule` holds the HTTP interceptor, error handling, and common UI components.

Routing is lazy-loaded per feature module — the analytics bundle is not loaded until the HR Manager navigates to that section.

**HTTP layer**

A single `ApiService` wraps `HttpClient` and enforces the response envelope contract. Feature services (`EmployeeService`, `CompensationService`, `AnalyticsService`) call `ApiService` and map responses to typed models. Components never call `HttpClient` directly.

**Salary visibility**

Salary fields are not fetched in the employee list. They are only requested when the HR Manager explicitly opens a compensation detail view. This is enforced at the API level (salary excluded from list responses) and reinforced in the UI by never displaying salary in the employee table.

---

## Database — PostgreSQL + Liquibase

Schema changes are managed exclusively through Liquibase changelogs. No manual DDL is ever run against a database directly. Changelogs live in `src/main/resources/db/changelog` and are applied automatically when the application starts.

Changelog structure:

```
db/changelog/
├── db.changelog-master.xml        (includes all others in order)
├── 0001-create-employees.xml
├── 0002-create-compensation-history.xml
└── 0003-seed-data.xml             (runs on 'local' and 'test' profiles only)
```

The seed changeset is gated by a Liquibase context (`local,test`) so it never runs in production.

---

## Testing Strategy

| Layer | Tool | Approach |
|---|---|---|
| Service (unit) | JUnit 5 + Mockito | Repositories mocked; no database required |
| Repository (integration) | Spring Boot Test + Testcontainers | Real PostgreSQL instance, known seed data |
| API (slice) | MockMvc + Spring Boot Test | Full HTTP stack, mocked service layer |
| Frontend (component) | Jest + Angular Testing Library | Isolated component rendering and interaction |
| E2E (stretch) | Cypress | Critical path: create employee → set salary → view analytics |

Testcontainers is used instead of H2 for repository tests. H2 does not support `PERCENTILE_CONT` or other PostgreSQL-specific syntax used in analytics queries. Running against a real engine catches SQL incompatibilities that H2 would silently accept or incorrectly reject.

---

## Local Development

```yaml
# docker-compose.yml provides:
# - PostgreSQL on port 5432
# - Angular dev server on port 4200 (ng serve)
# - Spring Boot on port 8080 (./mvnw spring-boot:run)
```

Liquibase runs on Spring Boot startup. The seed changeset populates 10,000 employees in the `local` profile. The Angular dev server proxies `/api` to `localhost:8080` to avoid CORS during development.

---

## Observability

Spring Boot Actuator exposes `GET /actuator/health` for uptime monitoring. Request logging via a servlet filter records method, path, status code, and duration. Salary fields are explicitly excluded from all log statements — this is enforced in the service layer, not left to caller discipline.
