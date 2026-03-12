# 🛡️ FRM Engine — Centralized Fraud Risk Management

> Production-grade Fraud Risk Management Engine for Fintech Payment Processing (BANK, NFS, IMPS, UPI)

[![Java](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-black)](https://kafka.apache.org/)

---

## 📋 About

The **FRM Engine** is a centralized, rule-based Fraud Risk Management system designed for a fintech company processing payments across **BANK**, **NFS** (National Financial Switch), **IMPS** (Immediate Payment Service), and **UPI** (Unified Payments Interface) channels.

It evaluates every inbound payment transaction in real-time using a configurable pipeline of **25 fraud detection rules**, assigns a composite risk score, and returns one of three decisions: **ALLOW**, **REVIEW**, or **BLOCK**.

---

## 📚 Documentation

See [`docs/FRM_ENGINE_COMPLETE_DOCUMENTATION.md`](docs/FRM_ENGINE_COMPLETE_DOCUMENTATION.md) for the complete HLD, architecture diagrams, database schema, API spec, and rollout plan.

> **"Why not just use Excel?"** — Great question. See [`docs/EXCEL_VS_FRM_ENGINE_COMPARISON.md`](docs/EXCEL_VS_FRM_ENGINE_COMPARISON.md) for a full 12-dimension comparison that explains exactly why a database-backed engine is the right choice for production payment fraud management.

---

## 🏗️ Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 17                             |
| Framework          | Spring Boot 3.2.3                   |
| Database           | PostgreSQL 15                       |
| Cache (L1)         | Caffeine (in-process)               |
| Cache (L2/Velocity)| Redis 7.x                           |
| Messaging          | Apache Kafka 3.x                    |
| DB Migration       | Flyway                              |
| Metrics            | Micrometer + Prometheus             |
| Build              | Maven 3.9+                          |

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3+

### 1. Clone & Configure

```bash
git clone https://github.com/himanshu0180singh/frm-engine.git
cd frm-engine
```

Copy and update database credentials in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/frm_db
    username: frm_user
    password: frm_password
```

### 2. Create Database

```sql
CREATE DATABASE frm_db;
CREATE USER frm_user WITH PASSWORD 'frm_password';
GRANT ALL PRIVILEGES ON DATABASE frm_db TO frm_user;
```

### 3. Build & Run

```bash
mvn clean package -DskipTests
java -jar target/frm-engine-1.0.0-SNAPSHOT.jar
```

Flyway will automatically run the DB migrations (`V1__init_frm_schema.sql`, `V2__seed_initial_data.sql`) on first startup.

---

## 🔌 API Usage

### Check a Transaction

**POST** `/api/v1/fraud/check`

```json
{
  "transactionId": "TXN-2024-001234",
  "customerId": "CUST-98765",
  "channel": "UPI",
  "transactionType": "PAYMENT",
  "amount": 75000.00,
  "currency": "INR",
  "payerAccountNumber": "9876543210",
  "beneficiaryVpa": "merchant@okaxis",
  "deviceId": "DEV-ABC-001",
  "ipAddress": "103.45.67.89"
}
```

**Response:**

```json
{
  "transactionId": "TXN-2024-001234",
  "customerId": "CUST-98765",
  "decision": "ALLOW",
  "totalScore": 80,
  "decisionReason": "",
  "ruleResults": [
    {
      "ruleCode": "AMT_HIGH",
      "ruleName": "High Single Transaction Amount",
      "fired": false,
      "score": 0
    }
  ],
  "evaluatedAt": "2024-03-09T13:00:00",
  "processingTimeMs": 12
}
```

---

## 🎯 Scoring Matrix

| Score Range | Decision | Action                            |
|-------------|----------|-----------------------------------|
| 0 – 299     | ALLOW    | Transaction proceeds              |
| 300 – 599   | REVIEW   | Flagged for analyst investigation |
| 600+        | BLOCK    | Transaction rejected immediately  |

---

## 📂 Project Structure

```
frm-engine/
├── docs/
│   └── FRM_ENGINE_COMPLETE_DOCUMENTATION.md
├── src/main/java/com/company/frm/
│   ├── FrmEngineApplication.java
│   ├── controller/        # REST API
│   ├── dto/               # Request/Response DTOs
│   ├── engine/            # Evaluation pipeline & checkers
│   │   └── rules/         # 25 fraud rule implementations
│   ├── entity/            # JPA entities (11 tables)
│   ├── enums/             # Channel, Decision, etc.
│   ├── event/             # Kafka event publishing
│   ├── repository/        # Spring Data JPA repos
│   └── service/           # Business services
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__init_frm_schema.sql
│       └── V2__seed_initial_data.sql
└── pom.xml
```

---

## 📊 Monitoring

Actuator endpoints are available at `/api/v1/actuator`:
- `/health` — Application health
- `/metrics` — Micrometer metrics
- `/prometheus` — Prometheus scrape endpoint

---

## 📜 License

Copyright © 2024 FRM Engine Contributors. All rights reserved.