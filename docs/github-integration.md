# GitHub Integration Guide

## Overview

This guide walks you through setting up CodeSage as a GitHub App to automatically review Pull Requests in your repositories.

## Prerequisites

- GitHub account with admin access to the target organization/repositories
- CodeSage backend deployed and accessible via HTTPS
- OpenAI or Claude API key

## Step 1: Create GitHub App

1. Go to your GitHub organization settings (or personal settings)
2. Navigate to **Developer settings** → **GitHub Apps** → **New GitHub App**

### Basic Information

- **GitHub App name**: `CodeSage` (or your preferred name)
- **Homepage URL**: Your CodeSage deployment URL
- **Webhook URL**: `https://your-domain.com/api/webhook/github`
- **Webhook secret**: Generate a random string (save this for later)

### Permissions

Set the following repository permissions:

- **Pull requests**: Read & Write
- **Contents**: Read only
- **Metadata**: Read only

### Subscribe to Events

Check these events:

- ✅ Pull request (opened, synchronize, reopened)

### Where can this GitHub App be installed?

- Select **Any account** (for public use) or **Only on this account** (for private use)

## Step 2: Generate Private Key

1. After creating the app, scroll to **Private keys** section
2. Click **Generate a private key**
3. Download the `.pem` file
4. Save it securely (you'll need this for authentication)

## Step 3: Install the App

1. Go to **Install App** in the left sidebar
2. Click **Install** next to your organization/account
3. Choose:
   - **All repositories** (for organization-wide use)
   - **Only select repositories** (for specific repos)
4. Click **Install**
5. Note the **Installation ID** from the URL (e.g., `https://github.com/settings/installations/12345678`)

## Step 4: Configure CodeSage Backend

Create a `.env` file or set environment variables:

```bash
# GitHub App Configuration
GITHUB_APP_ID=123456                    # From GitHub App settings
GITHUB_APP_PRIVATE_KEY_PATH=/path/to/private-key.pem
GITHUB_INSTALLATION_ID=987654           # From installation URL
GITHUB_WEBHOOK_SECRET=                  # From webhook configuration

# AI Provider
OPENAI_API_KEY=                         # Your OpenAI API key
```

## Step 5: Test the Integration

### 5.1 Create a Test PR

1. Create a new branch in your repository
2. Make some code changes
3. Open a Pull Request

### 5.2 Verify Webhook Delivery

1. Go to your GitHub App settings
2. Click **Advanced** → **Recent Deliveries**
3. You should see a `pull_request` event with status `200`

### 5.3 Check CodeSage Response

Within a few seconds, you should see:
- A comment from CodeSage on your PR
- Review results in the CodeSage dashboard

## Troubleshooting

### Webhook not received

**Check:**
- Is your backend publicly accessible?
- Is the webhook URL correct?
- Check firewall/security group settings

**Debug:**
```bash
# Check backend logs
docker logs codesage-backend

# Test webhook endpoint
curl -X POST https://your-domain.com/api/webhook/github \
  -H "X-GitHub-Event: ping" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Authentication errors

**Check:**
- Is the private key path correct?
- Is the App ID correct?
- Is the Installation ID correct?

**Debug:**
```bash
# Check environment variables
docker exec codesage-backend env | grep GITHUB
```

### No comment posted

**Check:**
- Does the app have "Pull requests: Write" permission?
- Is the app installed on the repository?
- Check backend logs for errors

## Security Best Practices

1. **Rotate webhook secret** periodically
2. **Restrict app installation** to necessary repositories only
3. **Store private key securely** (never commit to git)
4. **Use environment variables** for all secrets
5. **Enable webhook signature verification** (already implemented)

## Advanced Configuration

### Custom Review Rules

Edit `AIService.java` to customize the analysis prompt:

```java
private String buildAnalysisPrompt(String codeDiff) {
    return String.format("""
        Analyze this code with focus on:
        - Security vulnerabilities (OWASP Top 10)
        - Performance bottlenecks
        - Code maintainability
        ...
        """, codeDiff);
}
```

### Rate Limiting

GitHub API has rate limits:
- **5,000 requests/hour** for authenticated requests
- CodeSage caches installation tokens for 50 minutes
- Implements retry logic with exponential backoff

### Multiple Repositories

The same GitHub App can be used across multiple repositories. Each PR will be analyzed independently.

## Support

If you encounter issues:

1. Check backend logs: `docker logs codesage-backend`
2. Verify webhook deliveries in GitHub App settings
3. Test API endpoints manually
4. Review [GitHub Apps documentation](https://docs.github.com/en/apps)

---

**Next Steps:**
- [Deployment Guide](deployment.md)
- [API Documentation](api.md)
- [Contributing Guide](../CONTRIBUTING.md)
