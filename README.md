# CodeSage

[![CI](https://github.com/albonidrizi/code-sage/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/albonidrizi/code-sage/actions/workflows/ci.yml)
[![CodeQL](https://github.com/albonidrizi/code-sage/actions/workflows/codeql.yml/badge.svg?branch=main)](https://github.com/albonidrizi/code-sage/actions/workflows/codeql.yml)

CodeSage is a reference implementation of an asynchronous pull-request review service. It validates GitHub webhooks,
queues review work, prevents duplicate reviews, runs OpenAI, Claude, or deterministic demo analysis providers,
persists findings, exposes metrics, and presents results in a React dashboard.

It is a portfolio/reference project, not a hosted production service.

## Verified capabilities

- Constant-time HMAC-SHA256 GitHub webhook verification
- PostgreSQL-backed duplicate-review prevention
- RabbitMQ analysis, retry, and dead-letter queues
- OpenAI and Claude provider adapters with deterministic demo fallback
- AI provider and GitHub operation interfaces with prompt-injection-aware review prompts
- Micrometer/Prometheus review, provider, queue, token, and cost metrics
- Responsive review command-center dashboard
- CI coverage, dependency, secret, CodeQL, Trivy, Docker-build, and demo-smoke gates

See [architecture](docs/architecture.md), [threat model](docs/threat-model.md), [performance](docs/performance.md), and
[security policy](SECURITY.md).

## Public evidence

| Evidence | Status |
|---|---|
| Main-branch CI | [Passing run: backend, frontend, dependency audit, security, and Docker demo smoke](https://github.com/albonidrizi/code-sage/actions/runs/27479507566) |
| Code scanning | [Passing scheduled CodeQL run](https://github.com/albonidrizi/code-sage/actions/runs/27540400113) |
| Architecture decisions | [Deterministic demo provider](docs/decisions/0001-deterministic-demo-provider.md) and [queue retry/DLQ](docs/decisions/0002-queue-retry-and-dlq.md) |
| Metrics | [Prometheus metric surface and reproducible measurement command](docs/performance.md) |
| Hosted deployment, release, and demo video | Not currently published; use the reproducible Docker Compose demo below |

## Demo quick start

Requirements: Docker with Compose.

PowerShell:

```powershell
$env:CODESAGE_DEMO_POSTGRES_USER = "codesage"
$env:CODESAGE_DEMO_POSTGRES_PASSWORD = (New-Guid).Guid
$env:CODESAGE_DEMO_RABBITMQ_USERNAME = "codesage"
$env:CODESAGE_DEMO_RABBITMQ_PASSWORD = (New-Guid).Guid
docker compose -f docker-compose.demo.yml up -d --build --wait
curl.exe --fail -X POST http://localhost:8080/api/demo/reviews
curl.exe --fail http://localhost:8080/api/reviews/recent
```

Open the dashboard at <http://localhost:8088>. Stop and remove demo data with:

```bash
docker compose -f docker-compose.demo.yml down -v
```

The demo Compose file requires runtime-generated local credentials and does not commit sample passwords. Production
Compose requires explicit `POSTGRES_PASSWORD`, `RABBITMQ_USERNAME`, and `RABBITMQ_PASSWORD`, and keeps demo mode
disabled.

## Local verification

```bash
cd backend
mvn clean verify

cd ../frontend
npm ci
npm run lint
npm run test:coverage
npm run build
npm audit --audit-level=high
```

Verified on June 13, 2026:

| Surface | Result |
|---|---|
| Backend | 18 tests passed |
| Backend gated business core | JaCoCo gate met |
| Frontend | 4 tests passed |
| Frontend | 93.06% line, 85% branch |
| npm audit | 0 vulnerabilities |
| Docker Compose demo | Build, health checks, demo review POST, recent reviews API, and frontend HTTP passed |

## Configuration

Copy `.env.example` for production-like Compose configuration. Required secrets must be injected at runtime. Keep:

```text
GITHUB_WEBHOOK_SIGNATURE_REQUIRED=true
CODESAGE_DEMO_ENABLED=false
```

## Important limitations

- Remote provider calls require provider API keys and network egress; the deterministic provider remains the supported
  no-secrets demo path.
- Duplicate prevention keys on repository and PR number, not PR head SHA.
- DLQ reprocessing is manual.
- Actuator network access control must be enforced by the deployment platform.
- There is no hosted public deployment, tagged release, or demo video yet.

## Roadmap

1. Version reviews by PR head SHA.
2. Add Testcontainers PostgreSQL/RabbitMQ integration tests and reviewed DLQ replay tooling.
3. Measure and publish controlled webhook/review throughput results.
4. Add provider-specific prompt regression fixtures for known injection patterns.
