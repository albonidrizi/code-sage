# ADR 0001: Deterministic demo provider

## Status

Accepted

## Decision

Demo mode uses a deterministic `AIProvider` that analyzes known diff patterns without external credentials.

## Consequences

Demo workflows and tests are reproducible and free. The provider is intentionally not a substitute for a remote model
adapter, which remains future work.
