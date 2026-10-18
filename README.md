# ACME Compensation Hub

A production-oriented compensation management platform for HR teams to manage employee salary data and analyze organizational pay. Replaces error-prone spreadsheets with a structured, auditable system for up to 10,000 employees across multiple countries and currencies.

---

## What It Does

| Feature | Description |
|---|---|
| **Employee Directory** | Create, update, deactivate employees. Soft-delete preserves all history. |
| **Compensation Management** | Append-only salary history — every change is immutable and dated. No overwrites. |
| **Pay Analytics** | Headcount, average, median, min/max salary per department, scoped by country and currency. |

---

## Quick Start

### Prerequisites

- Docker + Docker Compose
- No local Java, Node, or PostgreSQL installation required — everything runs in containers.

### Run locally

```bash
git clone https://github.com/<your-org>/acme-salary-platform.git
cd acme-salary-platform

cp .env.example .env          # review defaults — change DB password for non-local use

docker compose up --build     # starts db + backend + frontend
```

| Service | URL |
|---|---|
| Frontend (Angular) | http://localhost:4200 |
| Backend API | http://localhost:8080/api/v1 |
| API Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 / db: `compensation` / user: `compensation` |

Liquibase runs automatically on backend startup and seeds 10,000 employees in the `local` profile.

---

## Project Layout

```
acme-salary-platform/
├── backend/                   Spring Boot 3 / Java 21
│   ├── src/main/java/         Application source
│   ├── src/test/java/         Unit + integration tests
│   └── src/main/resources/    Liquibase changelogs, application.properties
├── frontend/                  Angular 19 / Material Design
│   ├── src/app/               Feature modules (employees, compensation, analytics)
│   └── src/styles/            Design tokens, component theme overrides
├── docs/                      Architecture, data model, decisions, performance notes
├── config/                    Checkstyle and OWASP suppression config
├── docker-compose.yml         Full-stack local orchestration
└── .github/workflows/         CI (backend + frontend) and EC2 deploy pipeline
```

---

## Backend Development

```bash
cd backend

./mvnw spring-boot:run                           # run with local profile
./mvnw test -DexcludedGroups=integration         # unit tests only (fast, no Docker)
./mvnw verify                                    # unit tests + JaCoCo coverage gate (>=70% line)
./mvnw checkstyle:check                          # enforce Google Java Style
./mvnw spotbugs:check                            # bytecode-level bug detection
```

Coverage gate: **>= 70% line coverage, >= 60% branch coverage** enforced by JaCoCo on every CI run.

---

## Frontend Development

```bash
cd frontend

npm install
npm start                                        # dev server on http://localhost:4200
npm test -- --watch=false --browsers=ChromeHeadless   # 131 unit tests
npm run lint                                     # ESLint
npm run typecheck                                # TypeScript strict check
```

---

## API Reference

Base path: `/api/v1`

All responses follow the envelope:
```json
{ "data": ..., "meta": { ... }, "errors": [] }
```

| Method | Path | Description |
|---|---|---|
| `GET` | `/employees` | Paginated list with filters (department, country, status, search) |
| `POST` | `/employees` | Create employee |
| `GET` | `/employees/{id}` | Employee detail (salary excluded) |
| `PUT` | `/employees/{id}` | Update employee |
| `PATCH` | `/employees/{id}/deactivate` | Soft deactivate |
| `GET` | `/employees/{id}/compensation` | Current salary |
| `POST` | `/employees/{id}/compensation` | Record new salary entry |
| `GET` | `/employees/{id}/compensation/history` | Full salary history |
| `GET` | `/analytics/departments` | Department-level stats (headcount, avg, median, min, max) |
| `GET` | `/analytics/summary` | Organisation-wide summary |

---

## CI / CD

| Workflow | Trigger | What runs |
|---|---|---|
| `ci.yml` | push/PR to `main`, `develop` (backend) | Unit tests → JaCoCo gate → Checkstyle → SpotBugs → Trivy → OWASP DC → JAR build |
| `frontend-ci.yml` | push/PR to `main`, `develop` (frontend) | Typecheck → ESLint → Karma unit tests → `npm audit` → production build |
| `deploy.yml` | push to `main` | SSH to EC2 → `git pull` → `docker compose up -d --build` |

---

## Documentation

| Document | Purpose |
|---|---|
| [`docs/requirements.md`](docs/requirements.md) | Product requirements and MVP scope |
| [`docs/architecture.md`](docs/architecture.md) | System diagram, layering, API conventions |
| [`docs/data-model.md`](docs/data-model.md) | Schema, indexes, analytics SQL |
| [`docs/product-decisions.md`](docs/product-decisions.md) | Design decisions and trade-offs |
| [`docs/performance.md`](docs/performance.md) | Performance targets, index strategy, query design |
| [`docs/testing.md`](docs/testing.md) | Test coverage map and strategy |
| [`docs/ai-development.md`](docs/ai-development.md) | AI-assisted development approach and prompts |

---

## Key Design Decisions (summary)

- **Monolith** — One backend, one DB. Correct shape for 10,000 employees / single persona.
- **Append-only salary history** — Full audit trail. No overwrites, ever.
- **Salary excluded from list endpoints** — Sensitive data travels only when explicitly requested.
- **Native SQL for analytics** — `PERCENTILE_CONT` and window functions can't be expressed in JPQL.
- **Testcontainers over H2** — H2 doesn't support PostgreSQL analytic functions.
- **No currency conversion** — Salaries stored in native currency; analytics grouped by country+currency.
- **Liquibase for all DDL** — No manual SQL against any environment, ever.

See [`docs/product-decisions.md`](docs/product-decisions.md) for the full reasoning.
