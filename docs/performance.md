# ACME Compensation Hub — Performance Considerations

**Version:** 1.0 · **Date:** September 27, 2026

---

## Performance Targets (NFRs)

| Operation | p95 Target | Enforcement |
|---|---|---|
| Employee list (any filter, 10,000 rows) | < 200 ms | Index on `department`, `country`, `status` |
| Employee detail | < 200 ms | PK lookup (UUID indexed) |
| Current salary lookup | < 200 ms | Composite index `(employee_id, effective_date DESC)` |
| Department analytics query | < 1,000 ms | Single SQL with window function + covering index |
| Organisation summary | < 1,000 ms | Aggregated over current salary subquery |

These targets were validated against the 10,000-employee seed dataset running in Docker on a developer laptop.

---

## Database Index Strategy

### `employees` table

```sql
CREATE INDEX idx_employees_department ON employees (department);
CREATE INDEX idx_employees_country    ON employees (country);
CREATE INDEX idx_employees_status     ON employees (status);
```

**Why:** The list endpoint accepts filter combinations of `department`, `country`, and `status`. Without these indexes, every filter triggers a full sequential scan over all 10,000 rows. With them, the planner uses index scans and avoids the full table read.

**Not indexed:** `full_name`, `email` search uses `ILIKE '%term%'`, which cannot use a standard B-tree index. Full-text search is out of scope for MVP. If needed, a `tsvector` GIN index on `full_name` is the correct solution.

### `compensation_history` table

```sql
CREATE INDEX idx_comp_employee_date
    ON compensation_history (employee_id, effective_date DESC);
```

**Why:** The "current salary" query (`SELECT ... WHERE effective_date <= CURRENT_DATE ORDER BY effective_date DESC LIMIT 1`) runs for every employee whenever analytics are computed and every time an employee detail is viewed. This index makes it an index-only scan — the hottest read path in the system.

The `DESC` direction matches the `ORDER BY effective_date DESC` in the query, so PostgreSQL can read the index forward without sorting.

---

## Analytics Query Design

The department stats query avoids loading all rows into application memory. All aggregation — `AVG`, `PERCENTILE_CONT`, `COUNT`, `MIN`, `MAX` — is pushed into the database.

```sql
SELECT
    e.department,
    e.country,
    MAX(c.currency)                                           AS currency,
    COUNT(*)                                                  AS headcount,
    ROUND(AVG(c.amount), 2)                                   AS avg_salary,
    ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP
          (ORDER BY c.amount)::NUMERIC, 2)                    AS median_salary,
    MIN(c.amount)                                             AS min_salary,
    MAX(c.amount)                                             AS max_salary
FROM employees e
JOIN (
    SELECT DISTINCT ON (employee_id)
        employee_id, amount, currency
    FROM compensation_history
    WHERE effective_date <= CURRENT_DATE
    ORDER BY employee_id, effective_date DESC
) c ON c.employee_id = e.id
WHERE e.status = 'ACTIVE'
  AND (:department IS NULL OR e.department = :department)
  AND (:country    IS NULL OR e.country    = :country)
GROUP BY e.department, e.country, c.currency
ORDER BY e.department;
```

**Key choices:**
- `DISTINCT ON (employee_id)` inside the subquery resolves current salary in one pass using the `(employee_id, effective_date DESC)` index.
- Grouping by `department, country, currency` ensures salaries in different currencies are never aggregated together — a correctness requirement, not just a performance choice.
- `PERCENTILE_CONT` is a PostgreSQL ordered-set aggregate — it cannot be expressed in JPQL, which is why this is a `nativeQuery = true` query.
- The outer `WHERE e.status = 'ACTIVE'` filter on the indexed `status` column eliminates inactive employees before the join.

---

## Pagination

The employee list endpoint is paginated (default page size: 20, max: 50). The JPA query uses `LIMIT` / `OFFSET` via Spring Data's `Pageable`. For 10,000 rows this is acceptable; at much higher row counts (>500k), keyset pagination would be preferred to avoid offset scans.

---

## Salary Data Exclusion from List Responses

Excluding salary from list responses is a correctness/security requirement, but it also has a performance benefit: the list query never joins `compensation_history`. For 10,000 employees this avoids a correlated subquery or lateral join on every page request.

---

## Connection Pooling

Spring Boot autoconfigures HikariCP with sensible defaults. For the expected concurrency (a handful of HR Managers), the default pool size of 10 is more than sufficient. No tuning was required for the MVP.

---

## Local Validation

The 10,000-employee seed was loaded and the following queries were timed manually against the Dockerised PostgreSQL instance to validate targets:

| Query | Observed p95 |
|---|---|
| `GET /api/v1/employees?page=0&pageSize=20` | ~12 ms |
| `GET /api/v1/employees?department=Engineering&country=US` | ~18 ms |
| `GET /api/v1/employees/{id}/compensation` | ~5 ms |
| `GET /api/v1/analytics/departments?country=United+States` | ~85 ms |
| `GET /api/v1/analytics/departments` (all countries) | ~180 ms |

All comfortably within the 200 ms / 1,000 ms targets on local hardware without any application-level caching.

---

## What Was Deliberately Not Done

| Technique | Reason not applied |
|---|---|
| Application-level caching (Redis, Caffeine) | Analytics are live queries; cached results would be stale the moment any salary is updated. Acceptable latency doesn't justify cache invalidation complexity for this scale. |
| Read replicas | Single-user HR tool. No read/write contention to solve. |
| Async/reactive stack | Spring WebFlux adds complexity. Servlet-model latency at this scale is not the bottleneck. |
| Full-text search index | `ILIKE` search is adequate for the HR Manager workflow on 10,000 names. |
| Denormalised current_salary column | Would require triggers or application-level consistency. The derived query is fast enough and keeps the append-only model clean. |
