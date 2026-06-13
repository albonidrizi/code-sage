# ADR 0002: RabbitMQ retry and dead-letter queues

## Status

Accepted

## Decision

Rejected analysis messages move to a time-limited retry queue and then to a dead-letter queue rather than being
requeued indefinitely.

## Consequences

Poison messages stop consuming worker capacity. Operators still need a reviewed, explicit reprocessing procedure.
