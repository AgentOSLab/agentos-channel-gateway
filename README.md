# agentos-channel-gateway

**Channel Adapter Layer** for the AgentOS platform.

## Overview

`agentos-channel-gateway` handles protocol translation between external messaging platforms (Slack, WhatsApp, Microsoft Teams, Email, Webhooks) and the AgentOS Conversation service. It normalizes inbound messages into the AgentOS format and translates responses back to channel-specific formats.

**Port:** 8012 | **DB:** Redis only | **Status:** Scaffold

## Architecture Position

```
External Channels → [Channel Gateway :8012] → [Conversation :8011] → Execution Layer
Web UI           → [API Gateway :8080]      → [Conversation :8011] → Execution Layer
```

The Web UI connects directly through API Gateway. Only external messaging platforms route through Channel Gateway.

## Planned Responsibilities

| Component | Status | Description |
|-----------|--------|-------------|
| Slack Adapter | ⬜ Pending | Slack Events API + Web API integration |
| WhatsApp Adapter | ⬜ Pending | WhatsApp Business API integration |
| Teams Adapter | ⬜ Pending | Microsoft Teams Bot Framework integration |
| Email Adapter | ⬜ Pending | IMAP/SMTP email channel |
| Webhook Adapter | ⬜ Pending | Generic webhook receiver |
| Identity Mapper | ⬜ Pending | Map channel user IDs to AgentOS identities |
| Message Normalizer | ⬜ Pending | Convert channel-specific formats to unified format |

## Tech Stack

- Java 21 + Spring Boot 3.3.5 + WebFlux
- Redis (session mapping, rate limiting)

## Configuration

```yaml
agentos:
  services:
    conversation-url: http://localhost:8011
    user-system-url: http://localhost:8006
```

## Running Locally

```bash
mvn spring-boot:run
```

## Related

- [ADR-041: Conversation Orchestrator Separation](../../project/decision-log.md)
- [REQ-channel-gateway](../../docs/sdd/requirements/REQ-channel-gateway.md)
