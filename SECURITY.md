# Security Policy

Report vulnerabilities privately to the repository owner. Do not include secrets, private source code, tokens, or
customer payloads in public issues.

## Supported branch

Security fixes target `main`.

## Implemented controls

- GitHub webhook HMAC-SHA256 validation using constant-time comparison.
- Production Compose requires database and RabbitMQ credentials.
- Demo mode is explicit and uses isolated demo credentials.
- CORS origins are environment-configurable.
- Actuator exposes only health, info, and Prometheus; health details are hidden.
- External-service errors return sanitized client messages.
- CI gates high/critical Trivy dependency/filesystem findings, high npm advisories, and leaked secrets.

## Operational requirements

- Set a strong `GITHUB_WEBHOOK_SECRET` and leave `GITHUB_WEBHOOK_SIGNATURE_REQUIRED=true`.
- Keep `CODESAGE_DEMO_ENABLED=false` outside local demonstrations.
- Terminate TLS before the application and restrict Actuator/Prometheus at the network boundary.
- Rotate GitHub App, AI provider, database, and queue credentials after suspected exposure.
