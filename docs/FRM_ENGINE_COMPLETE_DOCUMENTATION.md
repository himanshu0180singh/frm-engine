# FRM Engine — Complete High-Level Design (HLD) Documentation

**Version:** 1.0.0  
**Date:** March 2024  
**Classification:** Internal – Confidential

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [System Architecture Diagram](#3-system-architecture-diagram)
4. [Component Description](#4-component-description)
5. [Database Schema](#5-database-schema)
6. [All 25 Fraud Rules Catalog](#6-all-25-fraud-rules-catalog)
7. [Scoring Matrix and Decision Logic](#7-scoring-matrix-and-decision-logic)
8. [End-to-End Data Flow](#8-end-to-end-data-flow)
9. [API Specification](#9-api-specification)
10. [Tech Stack](#10-tech-stack)
11. [Spring Boot Project Structure](#11-spring-boot-project-structure)
12. [4-Phase Rollout Plan](#12-4-phase-rollout-plan)
13. [Design Principles](#13-design-principles)

---

## 1. Executive Summary

### Company Context

This FRM (Fraud Risk Management) Engine is purpose-built for a **new fintech company** operating in the Indian payments ecosystem. The company provides payment infrastructure services across four primary channels:

| Channel | Description                                                   |
|---------|---------------------------------------------------------------|
| **UPI** | Unified Payments Interface — real-time P2P and P2M payments   |
| **IMPS**| Immediate Payment Service — 24×7 interbank fund transfers     |
| **NFS** | National Financial Switch — ATM and POS interoperability      |
| **BANK**| Direct bank account debit/credit transactions                 |

### Problem Statement

As transaction volumes scale, the risk of fraudulent activity — account takeovers, money mules, synthetic identity fraud, velocity attacks, and geo-spoofing — grows proportionally. Without a centralized fraud layer, each channel operates in isolation, creating blind spots and inconsistent enforcement.

### Solution

The FRM Engine provides a **single, centralized evaluation service** that:

- Intercepts every inbound transaction in **real time** (< 50ms p99 target)
- Evaluates up to **25 configurable fraud rules** per transaction
- Computes a **composite risk score**
- Returns one of three decisions: **ALLOW**, **REVIEW**, or **BLOCK**
- Maintains a full **immutable audit trail**
- Publishes **fraud events** to Kafka for downstream analytics and alerting
- Supports **shadow mode** for safe rule onboarding without production impact

---

## 2. Architecture Overview

The FRM Engine follows a **layered, pipeline-based architecture**:

```
Inbound Transaction
        │
        ▼
┌─────────────────┐
│  REST API Layer  │  ← FraudCheckController
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  Fraud Evaluation Pipeline   │
│  ┌──────────────────────┐   │
│  │  1. Sanity Check     │   │  ← Input validation
│  │  2. Blacklist Check  │   │  ← Fast-path BLOCK
│  │  3. Whitelist Check  │   │  ← Fast-path ALLOW
│  │  4. Rule Evaluation  │   │  ← All 25 rules
│  │  5. Score Aggregation│   │  ← Decision
│  └──────────────────────┘   │
└─────────────────────────────┘
         │
         ▼
┌──────────────────────┐
│  Response + Async    │
│  - Audit Log (DB)    │
│  - Kafka Events      │
│  - Alert Creation    │
└──────────────────────┘
```

---

## 3. System Architecture Diagram

```
                        ┌─────────────────────────────────────────────────────────────────┐
                        │                  CLIENT APPLICATIONS                            │
                        │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
                        │  │  UPI App │  │ IMPS App │  │ NFS App  │  │ Banking App  │  │
                        └──┴────┬─────┴──┴────┬─────┴──┴────┬─────┴──┴──────┬───────┘  
                                │              │              │               │            
                                └──────────────┴──────┬───────┴───────────────┘            
                                                       │ HTTPS POST /api/v1/fraud/check      
                                                       ▼                                    
                        ┌──────────────────────────────────────────────────────────────┐  
                        │                    LOAD BALANCER / API GATEWAY               │  
                        └────────────────────────────┬─────────────────────────────────┘  
                                                      │                                    
                        ┌─────────────────────────────▼────────────────────────────────┐  
                        │                   FRM ENGINE (Spring Boot 3.2)               │  
                        │                                                               │  
                        │  ┌─────────────────────────────────────────────────────────┐ │  
                        │  │              FraudCheckController (REST)                 │ │  
                        │  │              POST /fraud/check                           │ │  
                        │  └───────────────────────┬─────────────────────────────────┘ │  
                        │                          │                                    │  
                        │  ┌────────────────────────▼────────────────────────────────┐ │  
                        │  │           FraudEvaluationPipeline                        │ │  
                        │  │                                                           │ │  
                        │  │  Step 1: SanityChecker                                   │ │  
                        │  │          - Validate mandatory fields                     │ │  
                        │  │          - Check blacklist (fast-path BLOCK)             │ │  
                        │  │                                                           │ │  
                        │  │  Step 2: WhitelistChecker                                │ │  
                        │  │          - Check whitelist (fast-path ALLOW)             │ │  
                        │  │                                                           │ │  
                        │  │  Step 3: Rule Evaluation (25 rules)                      │ │  
                        │  │     ┌───────────┐ ┌──────────────┐ ┌─────────────────┐  │ │  
                        │  │     │ VELOCITY  │ │    AMOUNT    │ │   BEHAVIORAL    │  │ │  
                        │  │     │ Rules 1-4 │ │  Rules 5-8   │ │   Rules 9-12   │  │ │  
                        │  │     └───────────┘ └──────────────┘ └─────────────────┘  │ │  
                        │  │     ┌───────────┐ ┌──────────────┐ ┌─────────────────┐  │ │  
                        │  │     │  DEVICE   │ │     GEO      │ │    IDENTITY     │  │ │  
                        │  │     │Rules 13-15│ │ Rules 16-18  │ │  Rules 19-21   │  │ │  
                        │  │     └───────────┘ └──────────────┘ └─────────────────┘  │ │  
                        │  │     ┌───────────┐ ┌──────────────┐                       │ │  
                        │  │     │  NETWORK  │ │   PATTERN    │                       │ │  
                        │  │     │Rules 22-23│ │  Rules 24-25 │                       │ │  
                        │  │     └───────────┘ └──────────────┘                       │ │  
                        │  │                                                           │ │  
                        │  │  Step 4: RiskScoreAggregator                             │ │  
                        │  │          - Sum scores from fired rules                   │ │  
                        │  │          - Apply blocking rule override                  │ │  
                        │  │          - Determine ALLOW / REVIEW / BLOCK              │ │  
                        │  │                                                           │ │  
                        │  └────────────────────────┬────────────────────────────────┘ │  
                        │                           │                                    │  
                        │  ┌────────────────────────▼────────────────────────────────┐ │  
                        │  │                 Async Post-Processing                    │ │  
                        │  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │ │  
                        │  │  │ AuditService│  │ FraudEvent   │  │ AlertService  │  │ │  
                        │  │  │  (DB write) │  │  Publisher   │  │ (score≥300)   │  │ │  
                        │  │  └──────┬──────┘  └──────┬───────┘  └───────┬───────┘  │ │  
                        │  └─────────┼────────────────┼──────────────────┼───────────┘ │  
                        └────────────┼────────────────┼──────────────────┼─────────────┘  
                                     │                │                  │                  
                    ┌────────────────▼──┐   ┌─────────▼───────┐   ┌─────▼─────────────┐   
                    │   PostgreSQL 15   │   │  Apache Kafka   │   │  PostgreSQL 15    │   
                    │   frm_audit_log   │   │ frm.fraud.events│   │   frm_alert       │   
                    └───────────────────┘   └─────────────────┘   └───────────────────┘   
                                                     │                                      
                                            ┌────────▼────────┐                            
                                            │  Analytics /    │                            
                                            │  SIEM / ML      │                            
                                            └─────────────────┘                            

  ┌──────────────────────────────────────────────────────────────────────────────────────┐
  │                            SUPPORTING INFRASTRUCTURE                                 │
  │                                                                                      │
  │  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────────────────┐   │
  │  │   Redis 7.x     │  │   Caffeine L1    │  │   Prometheus + Grafana           │   │
  │  │  Velocity       │  │  Rule Config     │  │   Metrics & Alerting             │   │
  │  │  Tracking       │  │  Blacklist Cache │  │                                  │   │
  │  └─────────────────┘  └──────────────────┘  └──────────────────────────────────┘   │
  └──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Description

### 4.1 FraudCheckController
The sole REST endpoint accepting `POST /api/v1/fraud/check`. Validates the `TransactionRequest` payload and delegates to the `FraudEvaluationPipeline`. Returns a synchronous `FraudCheckResponse` containing the decision, total score, and individual rule results.

### 4.2 FraudEvaluationPipeline
The central orchestrator. Executes five ordered steps:
1. **SanityChecker** — Validates the request and performs blacklist fast-path check.
2. **WhitelistChecker** — Bypasses all rules for whitelisted entities.
3. **Rule Evaluation** — Loads channel-specific active rules from cache and evaluates each one.
4. **RiskScoreAggregator** — Sums scores, detects blocking rules, and maps to ALLOW/REVIEW/BLOCK.
5. **Async Post-Processing** — Persists audit log, publishes Kafka event, and creates alert if needed.

### 4.3 Rule Engine
Each rule implements the `FraudRule` interface:
- `isApplicable(request)` — Gate check to skip inapplicable rules.
- `evaluate(request)` — Returns a `RuleResult` with `fired`, `score`, and `reason`.

Rules extend `AbstractFraudRule` which provides parameter access helpers (`getIntParam`, `getDoubleParam`).

### 4.4 RuleConfigService
Loads `FrmRule` + `FrmRuleParameter` records from PostgreSQL, builds concrete rule instances, and caches the list per channel using Caffeine with a 5-minute TTL.

### 4.5 VelocityService
Uses Redis `INCR` + `EXPIRE` for sliding-window transaction counts. Keys are scoped to `frm:velocity:<customerId>:<windowSeconds>`.

### 4.6 BlacklistService / WhitelistChecker
Database-backed checks with Caffeine caching. Support permanent and time-bounded entries via `expires_at`.

### 4.7 AuditService
Saves `FrmAuditLog` records asynchronously using `@Async` (Spring TaskExecutor) to ensure the main transaction response is never delayed by audit writes.

### 4.8 FraudEventPublisher
Publishes `FraudEvent` objects to the Kafka topic `frm.fraud.events` for downstream consumption by analytics, SIEM, and ML pipelines.

---

## 5. Database Schema

The FRM Engine uses **11 PostgreSQL tables**. Migration is managed by Flyway.

### 5.1 Table Summary

| # | Table Name                  | Purpose                                       |
|---|-----------------------------|-----------------------------------------------|
| 1 | `frm_rule`                  | Catalog of all fraud detection rules          |
| 2 | `frm_rule_parameter`        | Configurable parameters per rule              |
| 3 | `frm_customer_profile`      | Running risk profile per customer             |
| 4 | `frm_audit_log`             | Immutable audit trail for every check         |
| 5 | `frm_blacklist`             | Blacklisted entities (accounts, IPs, devices) |
| 6 | `frm_whitelist`             | Whitelisted trusted entities                  |
| 7 | `frm_alert`                 | Fraud alerts raised for REVIEW/BLOCK          |
| 8 | `frm_global_config`         | Runtime engine configuration                  |
| 9 | `frm_channel_config`        | Per-channel scoring overrides                 |
|10 | `frm_rule_execution_stat`   | Rule performance metrics for tuning           |
|11 | `frm_case_note`             | Analyst notes on investigated cases           |

### 5.2 Key Table DDL

```sql
-- Rule catalog
CREATE TABLE frm_rule (
    id          BIGSERIAL PRIMARY KEY,
    rule_code   VARCHAR(50)  NOT NULL UNIQUE,
    rule_name   VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(50)  NOT NULL,   -- VELOCITY|AMOUNT|BEHAVIORAL|DEVICE|GEO|IDENTITY|NETWORK|PATTERN
    base_score  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_blocking BOOLEAN      NOT NULL DEFAULT FALSE,
    priority    INTEGER      NOT NULL DEFAULT 100,
    applies_to  VARCHAR(255),            -- Comma-separated channel list
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Audit trail
CREATE TABLE frm_audit_log (
    id               BIGSERIAL PRIMARY KEY,
    transaction_id   VARCHAR(100) NOT NULL,
    customer_id      VARCHAR(100) NOT NULL,
    channel          VARCHAR(50)  NOT NULL,
    txn_type         VARCHAR(50)  NOT NULL,
    amount           NUMERIC(20,2) NOT NULL,
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    final_decision   VARCHAR(20)  NOT NULL,
    total_score      INTEGER      NOT NULL,
    rules_fired      JSONB,
    request_payload  JSONB,
    response_payload JSONB,
    processing_ms    INTEGER,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

Full DDL is in `src/main/resources/db/migration/V1__init_frm_schema.sql`.

---

## 6. All 25 Fraud Rules Catalog

| # | Rule Code            | Category   | Base Score | Blocking | Description                                               |
|---|----------------------|------------|-----------|----------|-----------------------------------------------------------|
| 1 | VEL_COUNT_1H         | VELOCITY   | 200       | No       | More than N transactions in last 1 hour                   |
| 2 | VEL_AMOUNT_1H        | VELOCITY   | 250       | No       | Total amount exceeds threshold in 1 hour                  |
| 3 | VEL_COUNT_24H        | VELOCITY   | 150       | No       | More than N transactions in last 24 hours                 |
| 4 | VEL_NIGHT            | VELOCITY   | 180       | No       | Multiple transactions between 00:00 and 04:00             |
| 5 | AMT_HIGH             | AMOUNT     | 300       | No       | Single transaction amount exceeds defined threshold       |
| 6 | AMT_DEVIATION        | AMOUNT     | 220       | No       | Amount deviates significantly from customer average       |
| 7 | AMT_ROUND            | AMOUNT     | 80        | No       | Suspicious exact round-number amount                      |
| 8 | AMT_JUST_BELOW       | AMOUNT     | 180       | No       | Amount just below regulatory reporting limit              |
| 9 | BHDOT_DORMANT        | BEHAVIORAL | 250       | No       | Transaction from account dormant for 90+ days             |
|10 | BHDOT_NEWPAYEE       | BEHAVIORAL | 200       | No       | Large amount to payee added less than 24h ago             |
|11 | BHDOT_FOREIGN        | BEHAVIORAL | 200       | No       | Beneficiary account in foreign country                    |
|12 | BHDOT_CHANNEL_SWITCH | BEHAVIORAL | 150       | No       | Unusual channel switch after failed attempt               |
|13 | DEV_NEW              | DEVICE     | 150       | No       | Transaction from device not seen in last 30 days          |
|14 | DEV_MULTIPLE         | DEVICE     | 200       | No       | More than 2 devices used within 1 hour                    |
|15 | DEV_ROOTED           | DEVICE     | 350       | **Yes**  | Transaction from rooted/jailbroken device                 |
|16 | GEO_ANOMALY          | GEO        | 250       | No       | Transaction location far from last known location         |
|17 | GEO_BLOCKED_COUNTRY  | GEO        | 600       | **Yes**  | IP from sanctioned/blocked country                        |
|18 | GEO_VPN              | GEO        | 200       | No       | Request IP belongs to known VPN/proxy network             |
|19 | ID_BLACKLIST         | IDENTITY   | 700       | **Yes**  | Customer, account, IP, or device is blacklisted           |
|20 | ID_MISMATCH          | IDENTITY   | 300       | No       | Name/DOB mismatch between payer and registered profile    |
|21 | ID_MULTIPLE_FAIL     | IDENTITY   | 250       | No       | More than 3 auth failures in last 30 minutes              |
|22 | NET_SHARED_DEVICE    | NETWORK    | 300       | No       | Same device used by more than 3 accounts in 24h           |
|23 | NET_SHARED_IP        | NETWORK    | 250       | No       | Same IP used by more than 5 accounts in 1 hour            |
|24 | PAT_MULE             | PATTERN    | 350       | No       | Account exhibits money mule behavior                      |
|25 | PAT_BIN_ATTACK       | PATTERN    | 400       | **Yes**  | Multiple small transactions testing card validity         |

**Blocking rules** (5 total): DEV_ROOTED, GEO_BLOCKED_COUNTRY, ID_BLACKLIST, PAT_BIN_ATTACK — these always produce a BLOCK decision regardless of total score.

---

## 7. Scoring Matrix and Decision Logic

### Score Thresholds

```
┌─────────────────────────────────────────────────────────────────┐
│                    RISK SCORE → DECISION MAP                    │
├──────────────┬─────────────┬─────────────────────────────────────┤
│  Score Range │  Decision   │  Action                             │
├──────────────┼─────────────┼─────────────────────────────────────┤
│   0  – 299   │   ALLOW     │  Transaction proceeds normally      │
│  300 – 599   │   REVIEW    │  Flagged; analyst investigates      │
│  600+        │   BLOCK     │  Transaction rejected immediately   │
├──────────────┼─────────────┼─────────────────────────────────────┤
│  Any blocking│   BLOCK     │  Overrides score (immediate block)  │
│  rule fired  │             │                                     │
└──────────────┴─────────────┴─────────────────────────────────────┘
```

### Score Accumulation Example

| Rule Fired         | Score Added | Running Total | Decision |
|--------------------|-------------|---------------|----------|
| VEL_COUNT_1H       | +200        | 200           | ALLOW    |
| AMT_HIGH           | +300        | 500           | REVIEW   |
| BHDOT_DORMANT      | +250        | 750           | BLOCK    |

### Configuration

Thresholds are configurable per channel via `frm_channel_config` and globally via `frm_global_config`. They can also be overridden in `application.yml`:

```yaml
frm:
  engine:
    allow-threshold: 299
    review-threshold: 599
    block-threshold: 600
```

---

## 8. End-to-End Data Flow

```
T+0ms    Client sends POST /api/v1/fraud/check
          {transactionId, customerId, channel, amount, ...}
          │
T+1ms    FraudCheckController validates JSON payload
          @Valid annotation triggers Jakarta Validation
          │
T+2ms    FraudEvaluationPipeline.evaluate() begins
          Sets transactionTimestamp if absent
          │
T+3ms    SanityChecker.checkBlacklist()
          → Query Caffeine L1 cache for customer/IP/device
          → On miss: query PostgreSQL frm_blacklist
          → HIT: Return BLOCK immediately (score=700)
          → MISS: Proceed to whitelist check
          │
T+4ms    WhitelistChecker.isWhitelisted()
          → Query Caffeine L1 cache
          → HIT: Return ALLOW immediately (score=0)
          → MISS: Proceed to rule evaluation
          │
T+5ms    RuleConfigService.getActiveRulesForChannel(channel)
          → Query Caffeine L1 cache (TTL 5 min)
          → On miss: Load from PostgreSQL frm_rule + frm_rule_parameter
          │
T+6ms    Rule evaluation loop (up to 25 rules):
          For each applicable rule:
            ├── VelocityService → Redis INCR/GET
            ├── CustomerProfileService → PostgreSQL frm_customer_profile
            └── Rule.evaluate(request) → RuleResult
          │
T+8ms    RiskScoreAggregator.aggregate()
          Sum fired rule scores
          Check for blocking rules
          Map to ALLOW / REVIEW / BLOCK
          │
T+9ms    FraudCheckResponse returned to caller (synchronous)
          │
T+10ms   [ASYNC] AuditService.saveAuditAsync()
          INSERT INTO frm_audit_log (full request + response + score)
          │
T+10ms   [ASYNC] FraudEventPublisher.publish()
          Kafka produce to frm.fraud.events
          (if decision != ALLOW)
```

---

## 9. API Specification

### 9.1 Evaluate Transaction

**Endpoint:** `POST /api/v1/fraud/check`  
**Content-Type:** `application/json`

#### Request Body

```json
{
  "transactionId":          "TXN-2024-001234",
  "customerId":             "CUST-98765",
  "channel":                "UPI",
  "transactionType":        "PAYMENT",
  "amount":                 75000.00,
  "currency":               "INR",
  "payerAccountNumber":     "9876543210",
  "beneficiaryAccountNumber": "1234567890",
  "beneficiaryName":        "Merchant ABC",
  "beneficiaryIfsc":        "HDFC0001234",
  "beneficiaryVpa":         "merchant@okaxis",
  "deviceId":               "DEV-ABC-001",
  "ipAddress":              "103.45.67.89",
  "userAgent":              "PhonePe/4.2.0 Android/13",
  "latitude":               28.6139,
  "longitude":              77.2090,
  "transactionTimestamp":   "2024-03-09T13:00:00",
  "remarks":                "Monthly rent payment",
  "newPayee":               false,
  "authFailureCount":       0
}
```

#### Response — ALLOW

```json
{
  "transactionId":    "TXN-2024-001234",
  "customerId":       "CUST-98765",
  "decision":         "ALLOW",
  "totalScore":       80,
  "decisionReason":   "",
  "shadowMode":       false,
  "ruleResults": [
    {
      "ruleCode":  "AMT_HIGH",
      "ruleName":  "High Single Transaction Amount",
      "fired":     false,
      "score":     0,
      "reason":    null,
      "blocking":  false
    },
    {
      "ruleCode":  "VEL_NIGHT",
      "ruleName":  "Midnight Transaction Velocity",
      "fired":     false,
      "score":     0,
      "reason":    null,
      "blocking":  false
    }
  ],
  "evaluatedAt":      "2024-03-09T13:00:00.123",
  "processingTimeMs": 12
}
```

#### Response — BLOCK (Blacklist)

```json
{
  "transactionId":    "TXN-2024-001235",
  "customerId":       "CUST-00001",
  "decision":         "BLOCK",
  "totalScore":       700,
  "decisionReason":   "Customer ID is blacklisted: CUST-00001",
  "shadowMode":       false,
  "ruleResults": [
    {
      "ruleCode":  "ID_BLACKLIST",
      "ruleName":  "Blacklisted Entity",
      "fired":     true,
      "score":     700,
      "reason":    "Customer ID is blacklisted: CUST-00001",
      "blocking":  true
    }
  ],
  "evaluatedAt":      "2024-03-09T13:00:01.456",
  "processingTimeMs": 3
}
```

#### Response — REVIEW (Score-based)

```json
{
  "transactionId":    "TXN-2024-001236",
  "customerId":       "CUST-54321",
  "decision":         "REVIEW",
  "totalScore":       450,
  "decisionReason":   "VEL_COUNT_1H: count 12 reached limit 10 in 3600s; AMT_DEVIATION: amount 50000 is 5x above avg 9800",
  "shadowMode":       false,
  "ruleResults": [
    {
      "ruleCode":  "VEL_COUNT_1H",
      "ruleName":  "High Transaction Count (1h)",
      "fired":     true,
      "score":     200,
      "reason":    "Transaction count 12 reached limit 10 in 3600s window",
      "blocking":  false
    },
    {
      "ruleCode":  "AMT_DEVIATION",
      "ruleName":  "Amount Deviation from Avg",
      "fired":     true,
      "score":     220,
      "reason":    "Amount 50000.00 is 5.0x above customer average 9800.00",
      "blocking":  false
    }
  ],
  "evaluatedAt":      "2024-03-09T13:00:02.789",
  "processingTimeMs": 28
}
```

### 9.2 Health Check

**Endpoint:** `GET /api/v1/fraud/health`  
**Response:** `200 OK` — `"FRM Engine is running"`

### 9.3 Actuator Endpoints

| Endpoint                             | Description                  |
|--------------------------------------|------------------------------|
| `GET /api/v1/actuator/health`        | Application health status    |
| `GET /api/v1/actuator/metrics`       | All Micrometer metrics        |
| `GET /api/v1/actuator/prometheus`    | Prometheus scrape endpoint   |
| `GET /api/v1/actuator/info`          | Application metadata          |

---

## 10. Tech Stack

| Layer                 | Technology                  | Version    | Purpose                                    |
|-----------------------|-----------------------------|------------|--------------------------------------------|
| Language              | Java                        | 17 (LTS)   | Core application language                  |
| Framework             | Spring Boot                 | 3.2.3      | Application framework                      |
| Web                   | Spring Web MVC              | 3.2.3      | REST API server                            |
| ORM                   | Spring Data JPA / Hibernate | 3.2.3      | Database access & entity mapping           |
| Primary Database      | PostgreSQL                  | 15+        | Persistent store (rules, audit, profiles)  |
| In-process Cache      | Caffeine                    | 3.1.8      | L1 cache for rules, blacklist              |
| Distributed Cache     | Redis                       | 7.x        | L2 velocity tracking counters              |
| Messaging             | Apache Kafka                | 3.x        | Async fraud event streaming                |
| DB Migration          | Flyway                      | 10.x       | Schema versioning and seed data            |
| Metrics               | Micrometer + Prometheus     | 1.12.3     | Observability and alerting                 |
| Code Reduction        | Lombok                      | Latest     | Boilerplate elimination                    |
| Validation            | Jakarta Validation (Bean V.)| 3.x        | Input validation                           |
| Build Tool            | Maven                       | 3.9+       | Dependency management and build lifecycle  |
| Container             | Docker                      | Latest     | Containerization                           |

---

## 11. Spring Boot Project Structure

```
frm-engine/
├── .gitignore
├── pom.xml
├── README.md
├── docs/
│   └── FRM_ENGINE_COMPLETE_DOCUMENTATION.md
└── src/
    └── main/
        ├── java/com/company/frm/
        │   ├── FrmEngineApplication.java
        │   │
        │   ├── controller/
        │   │   └── FraudCheckController.java
        │   │
        │   ├── dto/
        │   │   ├── TransactionRequest.java
        │   │   ├── FraudCheckResponse.java
        │   │   └── RuleResult.java
        │   │
        │   ├── enums/
        │   │   ├── Channel.java          (UPI, IMPS, NFS, BANK)
        │   │   ├── Decision.java         (ALLOW, REVIEW, BLOCK)
        │   │   ├── TransactionType.java  (CREDIT, DEBIT, TRANSFER, ...)
        │   │   ├── EntityType.java       (CUSTOMER_ID, IP_ADDRESS, ...)
        │   │   └── AlertStatus.java      (OPEN, IN_REVIEW, RESOLVED_*, ...)
        │   │
        │   ├── entity/
        │   │   ├── FrmRule.java
        │   │   ├── FrmRuleParameter.java
        │   │   ├── FrmCustomerProfile.java
        │   │   ├── FrmAuditLog.java
        │   │   ├── FrmBlacklist.java
        │   │   ├── FrmWhitelist.java
        │   │   ├── FrmAlert.java
        │   │   └── FrmGlobalConfig.java
        │   │
        │   ├── repository/
        │   │   ├── RuleRepository.java
        │   │   ├── BlacklistRepository.java
        │   │   ├── WhitelistRepository.java
        │   │   ├── CustomerProfileRepository.java
        │   │   ├── AuditLogRepository.java
        │   │   ├── AlertRepository.java
        │   │   └── GlobalConfigRepository.java
        │   │
        │   ├── engine/
        │   │   ├── FraudEvaluationPipeline.java
        │   │   ├── SanityChecker.java
        │   │   ├── BlacklistChecker.java
        │   │   ├── WhitelistChecker.java
        │   │   └── rules/
        │   │       ├── FraudRule.java             (interface)
        │   │       ├── AbstractFraudRule.java     (base class)
        │   │       ├── HighAmountRule.java
        │   │       ├── VelocityCountRule.java
        │   │       ├── MidnightTxnRule.java
        │   │       ├── DormantAccountRule.java
        │   │       ├── AmountDeviationRule.java
        │   │       ├── GeoAnomalyRule.java
        │   │       └── RiskScoreAggregator.java
        │   │
        │   ├── service/
        │   │   ├── RuleConfigService.java
        │   │   ├── VelocityService.java
        │   │   ├── BlacklistService.java
        │   │   ├── CustomerProfileService.java
        │   │   └── AuditService.java
        │   │
        │   └── event/
        │       ├── FraudEvent.java
        │       └── FraudEventPublisher.java
        │
        └── resources/
            ├── application.yml
            └── db/migration/
                ├── V1__init_frm_schema.sql    (11 tables)
                └── V2__seed_initial_data.sql  (25 rules + seed data)
```

---

## 12. 4-Phase Rollout Plan

### Phase 1 — Shadow Mode (Weeks 1–4)

**Goal:** Deploy FRM Engine in parallel with zero production impact.

| Activity                                | Owner      | Duration |
|-----------------------------------------|------------|----------|
| Deploy to production infrastructure     | DevOps     | Week 1   |
| Enable `SHADOW_MODE_ENABLED=true`       | FRM Team   | Week 1   |
| Route 100% of traffic to FRM (no block) | DevOps     | Week 2   |
| Collect baseline metrics & rule signals | Analytics  | Week 3-4 |
| Tune rule thresholds from shadow data   | Risk Team  | Week 4   |

- All evaluations run but decision is **never enforced**
- Audit logs and Kafka events are written normally
- Zero customer impact; engineering gains confidence in latency and accuracy

---

### Phase 2 — Soft Enforcement (Weeks 5–8)

**Goal:** Enable BLOCK for highest-confidence rules only.

| Activity                                   | Owner     | Duration |
|--------------------------------------------|-----------|----------|
| Enable blocking for `ID_BLACKLIST` only    | FRM Team  | Week 5   |
| Enable blocking for `GEO_BLOCKED_COUNTRY`  | FRM Team  | Week 6   |
| Enable blocking for `PAT_BIN_ATTACK`       | FRM Team  | Week 6   |
| Enable REVIEW decision for score ≥ 400     | FRM Team  | Week 7   |
| Manual analyst review of flagged alerts    | Risk Ops  | Week 7-8 |
| Measure false positive rate; calibrate     | Analytics | Week 8   |

- Blocking rules cover < 1% of traffic (known-bad lists)
- REVIEW queue is staffed by risk analysts
- False positive feedback loop established

---

### Phase 3 — Full Enforcement (Weeks 9–12)

**Goal:** Enable all 25 rules with full ALLOW / REVIEW / BLOCK enforcement.

| Activity                                        | Owner     | Duration |
|-------------------------------------------------|-----------|----------|
| Enable all velocity rules (VEL_*)               | FRM Team  | Week 9   |
| Enable all amount rules (AMT_*)                 | FRM Team  | Week 9   |
| Enable behavioral & device rules                | FRM Team  | Week 10  |
| Enable network & pattern rules                  | FRM Team  | Week 10  |
| Set REVIEW threshold to 300, BLOCK to 600       | Risk Team | Week 11  |
| Integrate real-time alerts to risk ops          | DevOps    | Week 11  |
| Hypercare monitoring (15-min cadence)           | All       | Week 12  |

---

### Phase 4 — Optimization (Ongoing, Month 4+)

**Goal:** Continuous improvement through data-driven tuning.

| Activity                                            | Cadence   |
|-----------------------------------------------------|-----------|
| Weekly rule performance review (`frm_rule_execution_stat`) | Weekly  |
| Monthly threshold calibration                       | Monthly   |
| Quarterly ML-assisted rule scoring feedback         | Quarterly |
| Semi-annual new rule introduction                   | Semi-annual|
| Annual full architecture review                     | Annual    |

---

## 13. Design Principles

### 13.1 Fail-Safe Default
If the FRM Engine is unreachable or encounters an unhandled exception, the calling channel should default to **ALLOW** with the transaction flagged for async review. Fraud prevention must never become a payments outage.

### 13.2 Immutability of Audit Logs
`frm_audit_log` is append-only. No updates or deletes are permitted. This ensures forensic integrity for regulatory examination.

### 13.3 Zero Business Logic in Rules
Rules contain only detection logic. Business decisions (thresholds, limits) are driven by database-backed `frm_rule_parameter` records. Changes to thresholds require no code deployment.

### 13.4 Channel Isolation
Each channel (UPI, IMPS, NFS, BANK) can have independent scoring thresholds and active rule sets via `frm_channel_config` and the `applies_to` field on `frm_rule`.

### 13.5 Defense in Depth
Multiple independent rule categories (velocity, amount, behavioral, device, geo, identity, network, pattern) ensure that no single evasion technique defeats the system.

### 13.6 Observability First
Every rule firing is logged with timestamp and reason. Prometheus metrics expose real-time rule hit rates, average scores by channel, and p99 latency. Alerting is configured for anomalous spikes in BLOCK rate.

### 13.7 Horizontal Scalability
The FRM Engine is stateless at the application tier. All shared state (velocity counters, rule configs) lives in Redis or PostgreSQL. The service can be horizontally scaled behind a load balancer with no coordination required.

### 13.8 Shadow Mode for Safe Rollout
Every new rule or threshold change is tested in shadow mode before enforcement, eliminating the risk of a misconfigured rule causing incorrect blocks at scale.
