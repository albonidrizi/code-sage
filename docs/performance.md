# Performance

No production performance claims are made.

The repository exposes review duration, queue processing duration, provider success/failure/fallback, token usage,
estimated cost, completed reviews, failed reviews, and duplicate reviews through Micrometer/Prometheus.

## Reproducible smoke/load command

After starting demo mode:

```bash
docker compose -f docker-compose.demo.yml up -d --build --wait
for i in $(seq 1 25); do curl -sS -X POST http://localhost:8080/api/demo/reviews > /dev/null; done
curl -sS http://localhost:8080/actuator/prometheus | grep '^codesage_'
```

No measurement is recorded here because Docker Desktop was unavailable during the local verification run. Add
measured throughput and latency only after executing the command in a controlled environment.
