# GRC Platform Backend

**Governance, Risk & Compliance (GRC) management system** built with Spring Boot 4 / Java 17. Covers risk scenarios, assets, controls, compliance frameworks, audit campaigns, governance documents, threats, vulnerabilities, KRI monitoring, and treatment plans — with a full DevSecOps security pipeline implemented across 7 phases.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 4.0.3 |
| Security | Spring Security 7.0.4, JWT (HMAC-SHA256) |
| Persistence | PostgreSQL 16, Spring Data JPA, Hibernate |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Container | Docker (multi-stage), Docker Compose |
| Build | Maven 3.9 |
| CI/CD | GitHub Actions |

---

## Architecture

```
com.project.grcplatform
├── base/           # BaseEntity, AuditableEntity, SoftDeleteEntity
├── config/         # Security, CORS, JPA auditing, OpenAPI, MDC filter
├── constant/       # Enums for every status/type field
├── controller/     # REST controllers
├── dto/            # RequestDTO / ResponseDTO pairs
├── exception/      # Custom exceptions + global handler
├── mapper/         # Manual entity ↔ DTO mappers
├── model/          # JPA entities + History entities
├── repository/     # Spring Data JPA repositories
├── scanner/        # CVE/NVD integration
├── scheduler/      # Spring @Scheduled tasks
├── security/       # JWT utils, auth filter, auth token
├── seed/           # DB seeders
└── service/        # Business logic
```

### Base Entity Hierarchy

| Class | Fields added |
|---|---|
| `BaseEntity` | UUID id, `createdAt`, `updatedAt` |
| `AuditableEntity` | `createdBy`, `updatedBy` (Spring Data auditing) |
| `SoftDeleteEntity` | `deleted`, `deletedAt` (soft delete — no hard deletes) |

---

## Domain Modules

| Module | Base path |
|---|---|
| Risk Scenarios | `/api/scenarios` |
| Risk Assessments | `/api/assessments` |
| Risk Register | `/api/risk-register` |
| Risk Reports | `/api/reports` |
| Assets | `/api/assets` |
| Controls | `/api/controls` |
| Compliance | `/api/compliance/frameworks`, `/evaluations`, `/action-plans` |
| Audit | `/api/audit/campaigns`, `/findings`, `/recommendations` |
| Governance | `/api/governance/documents`, `/policies`, `/responsibilities` |
| Threats | `/api/threats`, `/api/threat-types` |
| Vulnerabilities | `/api/vulnerabilities`, `/api/scanners` |
| KRI | `/api/kri`, `/api/kri-categories` |
| Treatment Plans | `/api/treatment-plans` |
| Notifications | `/api/notifications` |
| Users & Roles | `/api/users`, `/api/roles`, `/api/permissions` |

---

## DevSecOps Security Pipeline

The project implements a 7-phase DevSecOps pipeline covering secrets management, SCA, SAST, container scanning, DAST, CI/CD automation, and observability.

---

### Phase 1 — Secrets Externalization

**Files changed:** `application.properties`, `JwtUtils.java`, `JwtAuthFilter.java`, `SecurityConfig.java`, `AuthController.java`, `EmailService.java`, `.gitignore`

All credentials and keys are externalized via environment variables — nothing sensitive is committed to source control.

| Variable | Description |
|---|---|
| `JWT_SECRET` | Base64-encoded 32-byte signing key (`openssl rand -base64 32`) |
| `MAIL_USERNAME` | Gmail sender address |
| `MAIL_PASSWORD` | Gmail App Password |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Database connection |
| `APP_FRONTEND_URL` / `APP_BACKEND_URL` | Used in email links |

Environment is selected by Spring profile:
- **Local dev** — `application-local.properties` (gitignored)
- **Docker/Compose** — `.env` file (gitignored); `.env.example` committed as template
- **CI/CD** — GitHub repository secrets

JWT is signed with an HMAC-SHA256 key loaded at startup via `@PostConstruct` in `JwtUtils`. The key is injected from `${JWT_SECRET}`, so it can be rotated without code changes. Token expiry: 1 hour.

---

### Phase 2 — OWASP Dependency-Check (SCA)

```bash
./mvnw dependency-check:check
```

- Fails the build on any dependency with **CVSS ≥ 7**
- Report: `target/dependency-check-report/dependency-check-report.html`
- Suppressions with justification and expiry dates in `owasp-suppressions.xml` (committed to git)
- OSS Index and .NET analyzers disabled (not applicable)
- Spring Security pinned to **7.0.4** via `<spring-security.version>` in `pom.xml` (overrides BOM default); suppression GAV patterns use `7\.0\.[0-9]+` to match both 7.0.3 and 7.0.4 since no patch is available yet
- All suppressions expire **2026-07-27** — scheduled for review when Spring Boot 4.0.4 / Spring Security 7.0.5 is released

Moved to a **nightly GitHub Actions workflow** (`owasp.yml`) to avoid blocking every push — the NVD database download (347K CVE records) consistently timed out at ~14% on per-push runs even with an NVD API key. The nightly job uses `actions/cache` to persist `~/.dependency-check-data` across runs.

---

### Phase 3 — SpotBugs + Find Security Bugs (SAST)

```bash
./mvnw compile spotbugs:check
```

| Setting | Value |
|---|---|
| Plugin | `spotbugs-maven-plugin` 4.9.3.0 (upgraded from 4.8.6.4 — required for JDK 24 / ASM 9.7) |
| Find Security Bugs | `findsecbugs-plugin` 1.13.0 |
| Effort | Max |
| Threshold | Low |
| `failOnError` | true |
| Include filter | `spotbugs-security-include.xml` — SECURITY + CORRECTNESS categories |
| Exclude filter | `spotbugs-exclude.xml` — justified false-positive suppressions |

**4 real vulnerabilities found and fixed:**

| File | Bug Pattern | Fix Applied |
|---|---|---|
| `GovernanceDocumentController.java` | `PATH_TRAVERSAL_IN` | Sanitize filename: `replaceAll("[^a-zA-Z0-9._-]", "_")` + `normalize()` + `startsWith(base)` containment check |
| `AuditService.java` | `UNSAFE_HASH_EQUALS` | Replace `==` with `MessageDigest.isEqual()` for constant-time comparison (prevents timing attacks) |
| `QualysAdapter.java` | `XXE_DOCUMENT` | Disable DOCTYPE declarations and external entity expansion on `DocumentBuilderFactory` |
| `CveImportService.java` | `CRLF_INJECTION_LOGS` | Strip `\r\n` from user-controlled `keyword` and `cveId` before logging |

**Suppressions in `spotbugs-exclude.xml`** (all documented with justification):

| Pattern | Reason |
|---|---|
| `SPRING_ENDPOINT` | Every `@GetMapping`/`@PostMapping` is intentional; not exploitable |
| `SPRING_CSRF_PROTECTION_DISABLED` | Stateless JWT API — CSRF does not apply |
| `IMPROPER_UNICODE` | False positives on standard string ops with no user-controlled Unicode paths |
| `CRLF_INJECTION_LOGS` (Low confidence) | Log statements using internal enum values or system-generated IDs |
| `CRLF_INJECTION_LOGS` in `RiskReportService` | Sanitized with `.replaceAll()` — FindSecBugs taint analysis does not recognize it as a sanitizer |

---

### Phase 4 — Docker + Trivy Container Scan

**Files added:** `Dockerfile`, `docker-compose.yml`, `.env.example`, `.trivyignore`

**Multi-stage `Dockerfile`:**
1. **Build stage** — Maven 3.9 + Eclipse Temurin 17: compiles and packages the JAR
2. **Runtime stage** — Temurin 17 JRE only: copies JAR, creates non-root `appuser`, runs as non-root

```bash
# Run the full stack
cp .env.example .env   # fill in secrets
docker compose up --build
```

`docker-compose.yml` starts the app and Postgres 16 with a health-check gate — the app container waits for Postgres to be healthy before starting.

**Trivy scan command:**
```bash
docker run --rm \
  -v "${PWD}:/scan" \
  -v "D:\TrivyCache:/root/.cache/trivy" \
  aquasec/trivy image \
  --timeout 10m \
  --input /scan/grc-app.tar \
  --exit-code 1 \
  --severity HIGH,CRITICAL
```

- Cache volume reused across scans — no re-downloading the Java vulnerability DB
- False-positive suppressions in `.trivyignore` with inline justification comments

---

### Phase 5 — OWASP ZAP (DAST)

**Files changed:** `SecurityConfig.java`

ZAP API scan run against the live application using the OpenAPI spec:

```bash
docker run --rm --network host \
  -v "${PWD}:/zap/wrk" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-api-scan.py \
  -t http://localhost:8080/v3/api-docs \
  -f openapi \
  -r zap-report.html \
  -I
```

**Final result: 0 HIGH, 0 MEDIUM** — 3 LOW alerts, all resolved.

| Alert | Severity | Fix |
|---|---|---|
| Cookie without SameSite attribute (`JSESSIONID`) | Low | Added `SessionCreationPolicy.STATELESS` — no session created, no JSESSIONID issued |
| Cross-Origin-Resource-Policy header missing | Low | Added `Cross-Origin-Resource-Policy: same-origin` via `StaticHeadersWriter` in `SecurityConfig` |
| Unexpected Content-Type on upload endpoint | Low | False positive — ZAP sent an oversized file; Spring returned HTML 413, real requests return JSON |

Informational only (no action):
- 955× `401` responses — expected, ZAP has no JWT token
- `Non-Storable Content` — correct, API responses must not be cached
- `Session Management / Authentication Identified` — informational

---

### Phase 6 — GitHub Actions CI/CD Pipeline

**Files:** `.github/workflows/ci.yml`, `.github/workflows/owasp.yml`

#### Per-push pipeline (`ci.yml`)

Runs on every push / PR to `master` — three sequential jobs:

| Job | Steps |
|---|---|
| `build` | Checkout → Java 17 → Maven build → Unit tests → SpotBugs SAST |
| `trivy` | Build Docker image → Trivy scan (fails on HIGH/CRITICAL) |
| `zap` | Start stack via `docker compose` → Wait for health → ZAP API scan → Upload HTML report artifact |

#### Nightly SCA pipeline (`owasp.yml`)

Runs at **02:00 UTC** every night (also `workflow_dispatch`):
- 90-minute timeout
- `actions/cache` persists `~/.dependency-check-data` across runs
- Separated from the per-push pipeline to avoid NVD rate limiting and download timeouts

**Required GitHub Secrets:**

| Secret | Description |
|---|---|
| `JWT_SECRET` | JWT signing key |
| `MAIL_USERNAME` | Gmail sender address |
| `MAIL_PASSWORD` | Gmail App Password |
| `DB_PASSWORD` | PostgreSQL password |
| `NVD_API_KEY` | NVD API key for nightly OWASP workflow |

---

### Phase 7 — Spring Actuator + Structured Logging

**Files added/changed:** `MdcCorrelationFilter.java`, `logback-spring.xml`, `application.properties`

#### Spring Actuator

Exposed endpoints: `health`, `info`, `metrics`, `prometheus`

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.endpoint.health.probes.enabled=true
```

Health detail is restricted to authenticated users — liveness and readiness probes are enabled for container orchestration.

#### MDC Correlation Filter

`MdcCorrelationFilter` runs at highest precedence and injects per-request context into every log line:

- Reads `X-Correlation-Id` request header — generates a UUID if absent
- Injects `correlationId`, `method`, `path`, and authenticated `user` into the MDC
- Echoes `X-Correlation-Id` back in the response header for end-to-end tracing
- Clears the MDC in a `finally` block to prevent context leakage between requests

#### Structured Logging

Profile-aware `logback-spring.xml`:

| Profile | Format |
|---|---|
| `local` | Human-readable colored pattern: `[correlationId] [user] logger - message` |
| All other profiles | **JSON / ECS format** via Spring Boot's `StructuredLoggingEncoder` (no extra dependency) |

ECS (Elastic Common Schema) output is compatible out of the box with the Elastic Stack (ELK), Grafana Loki, Datadog, and any log aggregation platform that understands structured JSON.

---

## Getting Started

### Prerequisites

- Java 17
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- PostgreSQL 16 (or Docker)

### Local Development

```bash
# 1. Copy and fill in environment variables
cp .env.example .env

# 2. Run with Docker Compose (app + Postgres)
docker compose up --build

# OR run directly with Maven (requires local Postgres)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API docs available at: `http://localhost:8080/swagger-ui/index.html`

Health check: `http://localhost:8080/actuator/health`

### Running the Security Pipeline Locally

```bash
# SAST
./mvnw compile spotbugs:check

# SCA
./mvnw dependency-check:check

# Build and container scan
docker build -t grc-platform-backend .
docker save grc-platform-backend -o grc-app.tar
docker run --rm -v "${PWD}:/scan" aquasec/trivy image --input /scan/grc-app.tar --severity HIGH,CRITICAL

# DAST (requires running stack)
docker compose up -d
docker run --rm --network host -v "${PWD}:/zap/wrk" ghcr.io/zaproxy/zaproxy:stable \
  zap-api-scan.py -t http://localhost:8080/v3/api-docs -f openapi -r zap-report.html -I
```

---

## Authentication

- JWT tokens signed with HMAC-SHA256, 1-hour expiry
- Sessions tracked in `user_session` table — token + active session must both be valid
- Login: max 5 attempts, 15-minute lockout
- MFA via OTP (`/api/auth/mfa/validate`)
- Role-based + permission-based access control with method-level security (`@EnableMethodSecurity`)
- Public endpoints: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- All other endpoints require a valid JWT — unauthenticated requests return `401`
