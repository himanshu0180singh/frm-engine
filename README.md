# FRM Engine — Centralized Fraud Risk Management

A production-ready **Fraud Risk Management (FRM) Engine** built with **Spring Boot 3.2 / Java 17**, serving BANK, NFS, IMPS, and UPI payment channels.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.3 (Java 17) |
| Database | PostgreSQL + Flyway |
| Cache | Redis (Lettuce) + Caffeine |
| Messaging | Apache Kafka |
| Monitoring | Micrometer + Prometheus |
| Build | Maven |

## Quick Start

### Prerequisites
- Java 17+, Maven 3.8+
- PostgreSQL, Redis, Kafka running locally

```bash
# 1. Clone and build
mvn clean package -DskipTests

# 2. Run
java -jar target/frm-engine-1.0.0.jar
```

### API

**Check a transaction:**
```bash
curl -X POST http://localhost:8080/api/v1/fraud/check \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN123456",
    "channel": "UPI",
    "senderId": "9876543210@upi",
    "receiverId": "merchant@upi",
    "amount": 250000,
    "currency": "INR",
    "txnTime": "2026-03-09T02:30:00"
  }'
```

**Health check:**
```bash
curl http://localhost:8080/api/v1/fraud/health
```

## Architecture Overview

```
[BANK / NFS / IMPS / UPI]
         |
    [API Gateway]
         |
  [Channel Adapter Layer]  ← normalises to TransactionRequest DTO
         |
   [Pre-Check Layer]       ← Sanity + Blacklist + Whitelist
         |
 [Rule Execution Engine]   ← 25 configurable fraud rules
         |
 [Risk Score Aggregator]   ← 0-1000 score → ALLOW / REVIEW / BLOCK
         |
  [Post-Decision Async]    ← Audit DB + Alerts + Kafka Events
```

## Phased Rollout

| Phase | Mode | Duration | Goal |
|---|---|---|---|
| 1 | SHADOW | Week 1-4 | Observe, build baselines |
| 2 | SOFT ENFORCE | Week 5-8 | Enable high-confidence rules only |
| 3 | FULL ENFORCE | Week 9-12 | All 25 rules active |
| 4 | OPTIMIZE | Week 13+ | ML scoring, risk tiers, dashboards |

## Documentation

Full HLD, database schema, rule catalog, and data flow:
👉 [docs/FRM_ENGINE_COMPLETE_DOCUMENTATION.md](docs/FRM_ENGINE_COMPLETE_DOCUMENTATION.md)

## License

MIT
