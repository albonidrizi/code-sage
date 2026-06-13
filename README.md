# CodeSage

CodeSage is a reference implementation of an asynchronous pull-request review service. It validates GitHub webhooks,
queues review work, prevents duplicate reviews, runs a deterministic analysis provider, persists findings, exposes
metrics, and presents results in a React dashboard.

It is a portfolio/reference project, not a hosted production service.

## Verified capabilities

- Constant-time HMAC-SHA256 GitHub webhook verification
- PostgreSQL-backed duplicate-review prevention
- RabbitMQ analysis, retry, and dead-letter queues
- Deterministic demo workflow without GitHub or AI-provider secrets
- AI provider and GitHub operation interfaces
- Micrometer/Prometheus review, provider, queue, token, and cost metrics
- Responsive review command-center dashboard
- CI coverage, dependency, secret, CodeQL, Trivy, Docker-build, and demo-smoke gates

See [architecture](docs/architecture.md), [threat model](docs/threat-model.md), [performance](docs/performance.md), and
[security policy](SECURITY.md).

## Demo quick start

Requirements: Docker with Compose.

```bash
docker compose -f docker-compose.demo.yml up -d --build --wait
curl --fail -X POST http://localhost:8080/api/demo/reviews
curl --fail http://localhost:8080/api/reviews/recent
```

Open the dashboard at <http://localhost:8088>. Stop and remove demo data with:

```bash
docker compose -f docker-compose.demo.yml down -v
```

The demo Compose file contains local-only credentials. Production Compose requires explicit `POSTGRES_PASSWORD`,
`RABBITMQ_USERNAME`, and `RABBITMQ_PASSWORD`, and keeps demo mode disabled.

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
| Backend | 13 tests passed |
| Backend gated business core | 97.8% line, 82.1% branch |
| Frontend | 4 tests passed |
| Frontend | 93.06% line, 85% branch |
| npm audit | 0 vulnerabilities |
| Docker Compose config | Demo configuration valid |

Docker startup and the demo HTTP workflow were not executed locally because Docker Desktop was unavailable. CI runs
that workflow on Linux.

## Configuration

Copy `.env.example` for production-like Compose configuration. Required secrets must be injected at runtime. Keep:

```text
GITHUB_WEBHOOK_SIGNATURE_REQUIRED=true
CODESAGE_DEMO_ENABLED=false
```

## Important limitations

- The new provider interface currently ships only with the deterministic demo provider; remote provider adapters are
  future work.
- Duplicate prevention keys on repository and PR number, not PR head SHA.
- DLQ reprocessing is manual.
- Actuator network access control must be enforced by the deployment platform.
- The latest public GitHub Actions run predates these local changes and is failing; a new run is required after push.

## Roadmap

1. Add hardened remote AI provider adapters with configurable retry, timeout, and circuit-breaker policies.
2. Version reviews by PR head SHA.
3. Add Testcontainers PostgreSQL/RabbitMQ integration tests and reviewed DLQ replay tooling.
4. Measure and publish controlled webhook/review throughput results.
