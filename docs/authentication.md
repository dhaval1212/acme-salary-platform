# Authentication Architecture

## Overview

ACME Compensation Hub uses stateless JWT (JSON Web Token) authentication. There is no public registration endpoint. Only explicitly provisioned users with valid credentials can log in and access the protected API.

## Login API

```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "hr.manager",
  "password": "changeme123"
}

Success (200):
{
  "data": {
    "token": "eyJhbGci...",
    "expiresAt": "2026-10-18T15:30:00Z",
    "user": {
      "id": "00000000-0000-0000-0000-000000000001",
      "username": "hr.manager",
      "fullName": "HR Manager",
      "role": "HR_MANAGER"
    }
  }
}

Failure (401):
{
  "errors": [
    { "field": "global", "message": "Invalid credentials" }
  ]
}
```

The error message is identical for wrong passwords, unknown usernames, and disabled accounts to prevent user enumeration.

## Logout API

```
POST /api/auth/logout
Content-Type: application/json

Success (200):
{
  "data": {
    "message": "Logged out successfully"
  }
}
```

Client clears in-memory JWT tokens and session state. The server provides both `/api/auth/logout` and `/api/v1/auth/logout` endpoints.

## User Provisioning

Users are stored in the `users` table. There is no public signup API.

For local development and tests, a seed user is created by Liquibase changeset `0006-seed-users.xml` (context: `local,test`):

| Username      | Password       | Role       | Enabled |
|---------------|----------------|------------|---------|
| `hr.manager`  | `changeme123`  | HR_MANAGER | true    |
| `disabled.user` | `changeme123` | HR_MANAGER | false |

For production, users must be provisioned by an administrator with direct database access or a future admin command. Passwords are stored as BCrypt hashes (cost factor 12) — never plaintext.

## JWT Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (required) | HMAC-SHA256 signing key, minimum 32 characters |
| `JWT_EXPIRATION_MS` | `3600000` (1 hour) | Token lifetime in milliseconds |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated list of allowed frontend origins |

Generate a strong secret:
```bash
openssl rand -hex 32
```

The application fails to start if `JWT_SECRET` is missing or shorter than 32 characters.

## Token Storage

The frontend stores the JWT in memory (a private `BehaviorSubject` in `AuthService`), not in `localStorage` or `sessionStorage`. This mitigates XSS attacks that could steal a persisted token.

Trade-off: a page reload clears the token and the user must log in again. This is acceptable for a short-lived access token on an HR tool used actively in a browser session.

## How to Run Locally

```bash
# 1. Copy environment template
cp .env.example .env
# Fill in JWT_SECRET with: openssl rand -hex 32

# 2. Start with Docker Compose
docker compose up --build

# 3. Access the app
# Frontend: http://localhost:4200
# Login with: hr.manager / changeme123
```

For development without Docker:
```bash
# Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (proxy handles /api to :8080)
cd frontend
npm start
```

## Key Rotation

To rotate the signing key:
1. Generate a new secret: `openssl rand -hex 32`
2. Update `JWT_SECRET` in `.env` or your deployment environment
3. Restart the backend — all existing tokens become invalid immediately
4. Users will need to log in again with the new tokens

## Disabling a Compromised Account

```sql
UPDATE users SET enabled = false WHERE username = 'compromised.username';
```

Disabled accounts cannot log in. The login endpoint returns the same generic "Invalid credentials" message as a wrong password.

## Running Authentication Tests

```bash
# Backend unit tests (no Docker needed)
cd backend
./mvnw test -DexcludedGroups=integration

# Backend integration tests (Docker required)
./mvnw test -Dgroups=integration

# Frontend tests
cd frontend
npm run test:ci
```

## Security Notes

- CSRF is disabled — correct for stateless Bearer-token APIs (CSRF attacks require cookie-based auth)
- CORS is narrowly configured for the actual frontend origin(s)
- Passwords, tokens, and signing secrets are never logged
- The token is only attached to requests targeting the API base URL, never third-party origins
- Route guards are a UX measure only — the backend independently enforces authentication on every request
- Expired and tampered tokens are rejected with a 401 response
- Unsigned tokens ("alg:none") are rejected by the JJWT library

## Current Limitations

- No refresh token mechanism — users must re-login after token expiry
- No registration endpoint — users are provisioned via database seed or direct SQL
- Single role (HR_MANAGER) — role-based access control can be expanded but is not yet implemented
- Token is in-memory only — page reload requires re-login
