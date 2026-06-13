# Deployment Guide

## Quick Deploy Options

### Option 1: Railway (Recommended for Beginners)

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/new)

1. Click the button above
2. Connect your GitHub repository
3. Add environment variables (see `.env.example`)
4. Deploy!

**Cost**: ~$5-10/month for hobby projects

### Option 2: Render

1. Go to [Render Dashboard](https://dashboard.render.com/)
2. Create **New PostgreSQL** database
3. Create **New Web Service** for backend
   - Build Command: `cd backend && mvn clean package -DskipTests`
   - Start Command: `java -jar backend/target/*.jar`
4. Create **New Static Site** for frontend
   - Build Command: `cd frontend && npm install && npm run build`
   - Publish Directory: `frontend/dist`

**Cost**: Free tier available

### Option 3: Docker Compose (Self-Hosted)

Perfect for VPS (DigitalOcean, Linode, AWS EC2):

```bash
# Clone repository
git clone https://github.com/yourusername/CodeSage.git
cd CodeSage

# Copy environment template
cp .env.example .env

# Edit .env with your API keys
nano .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f
```

**Cost**: $5-20/month depending on VPS provider

## Detailed Setup

### Prerequisites

- Domain name (optional but recommended)
- SSL certificate (use Let's Encrypt)
- OpenAI or Claude API key
- GitHub App (for PR integration)

### Environment Variables

Required variables:

```bash
# AI Provider (at least one required)
OPENAI_API_KEY=
# OR
CLAUDE_API_KEY=

# GitHub App (optional for local dev)
GITHUB_APP_ID=123456
GITHUB_APP_PRIVATE_KEY_PATH=/path/to/key.pem
GITHUB_INSTALLATION_ID=987654
GITHUB_WEBHOOK_SECRET=

# Database
POSTGRES_PASSWORD=

# RabbitMQ
RABBITMQ_PASSWORD=
```

### SSL/TLS Setup

For production, use a reverse proxy (nginx/Caddy) with SSL:

```nginx
server {
    listen 443 ssl http2;
    server_name codesage.yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    # Backend API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Frontend
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
    }
}
```

### Health Checks

Monitor your deployment:

```bash
# Backend health
curl https://your-domain.com/api/reviews/health

# Expected response:
{
  "status": "UP",
  "service": "CodeSage Review API",
  "timestamp": "2025-12-09T...",
  "totalReviews": 42
}
```

### Scaling Considerations

For high-traffic deployments:

1. **Database**: Use managed PostgreSQL (AWS RDS, DigitalOcean Managed DB)
2. **Message Queue**: Use managed RabbitMQ (CloudAMQP)
3. **Backend**: Scale horizontally with load balancer
4. **Frontend**: Use CDN (Cloudflare, AWS CloudFront)

### Backup Strategy

```bash
# Backup PostgreSQL
docker exec codesage-db pg_dump -U postgres codesage > backup.sql

# Restore
docker exec -i codesage-db psql -U postgres codesage < backup.sql
```

### Monitoring

Use these tools:

- **Uptime**: UptimeRobot, Pingdom
- **Logs**: Papertrail, Logtail
- **Metrics**: Prometheus + Grafana
- **Errors**: Sentry

### Cost Estimation

| Component | Free Tier | Paid (Small) | Paid (Medium) |
|-----------|-----------|--------------|---------------|
| **Hosting** | Render | Railway $5 | DigitalOcean $20 |
| **Database** | Render (512MB) | Railway $5 | DO Managed $15 |
| **AI API** | OpenAI $0-5 | OpenAI $10-50 | OpenAI $50-200 |
| **Total** | $0-5/mo | $20-60/mo | $85-235/mo |

### Security Checklist

- [ ] Use HTTPS everywhere
- [ ] Rotate secrets regularly
- [ ] Enable firewall rules
- [ ] Use environment variables (never hardcode secrets)
- [ ] Keep dependencies updated
- [ ] Enable GitHub webhook signature verification
- [ ] Use strong database passwords
- [ ] Restrict CORS origins in production

### Troubleshooting

**Backend won't start:**
```bash
# Check logs
docker logs codesage-backend

# Common issues:
# - Database connection failed → Check POSTGRES_PASSWORD
# - RabbitMQ connection failed → Check RABBITMQ_HOST
# - Port already in use → Change port in docker-compose.yml
```

**Frontend shows errors:**
```bash
# Check if backend is accessible
curl http://localhost:8080/api/reviews/health

# Check CORS settings in application.properties
```

**GitHub webhooks failing:**
```bash
# Test webhook endpoint
curl -X POST https://your-domain.com/api/webhook/github \
  -H "X-GitHub-Event: ping" \
  -d '{}'

# Check webhook secret matches
```

## Production Checklist

Before going live:

- [ ] Set up SSL/TLS
- [ ] Configure domain name
- [ ] Set all environment variables
- [ ] Test GitHub App integration
- [ ] Set up monitoring
- [ ] Configure backups
- [ ] Test error scenarios
- [ ] Load test with expected traffic
- [ ] Document runbook for incidents

---

**Need help?** Open an issue on GitHub or contact support.
