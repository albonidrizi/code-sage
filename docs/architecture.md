# Architecture

CodeSage accepts GitHub pull-request events, queues review work, fetches a diff, analyzes it through an `AIProvider`,
persists the result, and posts a review comment.

```mermaid
flowchart LR
  GH[GitHub webhook] -->|HMAC validated| API[Webhook controller]
  API --> MQ[RabbitMQ analysis queue]
  MQ --> ORCH[Review orchestrator]
  ORCH -->|deduplicate| DB[(PostgreSQL)]
  ORCH --> GHO[GitHubOperations]
  ORCH --> AIP[AIProvider chain]
  AIP --> OAI[OpenAI provider]
  AIP --> CLAUDE[Claude provider]
  AIP --> DEMO[Deterministic demo provider]
  ORCH --> METRICS[Micrometer / Prometheus]
```

```mermaid
sequenceDiagram
  participant G as GitHub
  participant W as Webhook API
  participant Q as RabbitMQ
  participant R as ReviewOrchestrator
  participant A as AIProvider
  participant D as PostgreSQL
  G->>W: pull_request + signature
  W->>W: constant-time signature verification
  W->>Q: publish event
  Q->>R: consume event
  R->>D: check/create unique review
  R->>A: analyze diff
  A-->>R: structured review result
  R->>D: persist result
  R-->>G: post review comment
```

## Boundaries

- `AIProvider` isolates analysis implementations and fallback order.
- `GitHubOperations` isolates external GitHub behavior.
- `ReviewOrchestrator` owns duplicate prevention and the end-to-end review transaction.
- RabbitMQ routes rejected messages through a retry queue and then a dead-letter queue.

## Trade-offs

- Remote OpenAI and Claude providers use bounded diff input, JSON-only review prompts, timeout, retry, and circuit
  breaker policies. The deterministic provider remains the no-secrets fallback for demo and repeatable tests.
- The database uniqueness constraint prevents concurrent duplicate reviews for one repository/PR number. A future
  version should include the PR head SHA to permit a new review after synchronization.
