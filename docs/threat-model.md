# Threat Model

## Assets

- GitHub App credentials and installation tokens
- AI provider credentials
- Private pull-request diffs and review findings
- Database and RabbitMQ credentials

## Trust boundaries

1. Internet to webhook endpoint
2. Backend to GitHub and AI providers
3. Backend to RabbitMQ and PostgreSQL
4. Operator to Actuator/Prometheus

## Primary threats and controls

| Threat | Impact | Control | Residual risk |
|---|---|---|---|
| Forged webhook | Unauthorized analysis and queue abuse | HMAC-SHA256 and constant-time comparison | Secret compromise |
| Duplicate delivery | Duplicate cost/comments | Repository/PR uniqueness plus orchestrator duplicate check | Synchronize events need head-SHA versioning |
| Poison message | Worker retry loop | Retry queue, DLQ, no infinite requeue | Manual DLQ reprocessing is not implemented |
| Secret leakage | Credential/source exposure | Sanitized errors, no payload/source logging | Third-party SDK logging must remain reviewed |
| Untrusted diff prompt injection | Manipulated AI review | Deterministic demo provider; provider abstraction | Remote-provider prompt hardening is pending |
| Public Actuator details | Infrastructure disclosure | Limited exposure and hidden health details | Network restriction is deployment-owned |

## Security verification

- `npm audit --audit-level=high`
- CI Trivy filesystem gate
- CI OWASP Maven dependency check
- CI Gitleaks scan
- CodeQL workflow
