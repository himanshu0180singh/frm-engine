# Centralized Fraud Risk Management (FRM) Engine — Complete Technical Documentation

**Version:** 1.0.0  
**Date:** March 2026  
**Company:** [Company Name] — Fintech Payment Processor  
**Classification:** Internal Technical Document

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [High-Level Architecture (HLD)](#2-high-level-architecture-hld)
3. [Complete Database Schema](#3-complete-database-schema)
4. [Seed Data](#4-seed-data)
5. [Spring Boot Project Structure](#5-spring-boot-project-structure)
6. [Complete Fraud Rules Catalog (25 Rules)](#6-complete-fraud-rules-catalog-25-rules)
7. [Scoring Matrix and Decision Logic](#7-scoring-matrix-and-decision-logic)
8. [Complete Data Flow — End-to-End](#8-complete-data-flow--end-to-end)
9. [Tech Stack Summary](#9-tech-stack-summary)
10. [Phased Rollout Plan](#10-phased-rollout-plan)
11. [API Specification](#11-api-specification)
12. [Key Design Principles](#12-key-design-principles)

---

## 1. Executive Summary

### What is the FRM Engine?

The **Centralized Fraud Risk Management (FRM) Engine** is a real-time, rule-based transaction screening system that evaluates every payment transaction before it is processed or immediately after authorization. It assigns a numerical risk score to each transaction and returns one of three decisions: **ALLOW**, **REVIEW**, or **BLOCK**.

### Why is it needed?

As a brand new fintech payment processor, the company starts with **zero fraud history** and **zero customer behavioral baselines**. Without a fraud detection layer, the company is immediately exposed to:

- Account takeover attacks on new customers
- Card/UPI credential testing (micro-transaction probing)
- Money mule networks exploiting new accounts
- Regulatory non-compliance (RBI mandates fraud monitoring for payment processors)

The FRM Engine addresses this by operating in **SHADOW MODE** initially — evaluating and logging all transactions without blocking — to build behavioral baselines before enforcement begins.

### Channels Supported

| Channel | Description | Protocol |
|---------|-------------|----------|
| **BANK** | Core Banking — NEFT/RTGS wire transfers | ISO 20022 XML |
| **NFS** | National Financial Switch — ATM withdrawals | ISO 8583 |
| **IMPS** | Immediate Payment Service — real-time P2P | JSON REST |
| **UPI** | Unified Payments Interface — VPA-based payments | UPI XML / JSON |

### Key Capabilities

- **Real-time evaluation** — target < 25ms for the synchronous fraud check response
- **Rule-based scoring** — 25 configurable rules with DB-managed thresholds
- **Shadow mode** — safe onboarding for new companies with zero false-positive risk
- **Channel agnostic core** — adapter pattern normalises all channel payloads to a single DTO
- **Fail-safe** — Redis failures default to REVIEW, not ALLOW
- **Full audit trail** — every decision persisted for RBI/PCI-DSS compliance

---

## 2. High-Level Architecture (HLD)

### ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        EXTERNAL CHANNEL SOURCES                             │
│                                                                             │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────────────┐   │
│   │   BANK   │   │   NFS    │   │   IMPS   │   │        UPI           │   │
│   │ NEFT/RTGS│   │ATM/ISO   │   │  JSON    │   │  VPA / UPI XML       │   │
│   │ ISO 20022│   │  8583    │   │  REST    │   │  Collect/Pay         │   │
│   └────┬─────┘   └────┬─────┘   └────┬─────┘   └──────────┬───────────┘   │
└────────┼──────────────┼──────────────┼────────────────────┼───────────────┘
         │              │              │                    │
         └──────────────┴──────────────┴────────────────────┘
                                       │
                        ┌──────────────▼──────────────┐
                        │     API GATEWAY / LB         │
                        │  • Rate limiting             │
                        │  • SSL termination           │
                        │  • JWT authentication        │
                        │  • Channel routing           │
                        │  • Health checks             │
                        └──────────────┬──────────────┘
                                       │
                        ┌──────────────▼──────────────────────────────────────┐
                        │              FRM ENGINE CORE                        │
                        │                                                     │
                        │  ┌────────────────────────────────────────────┐    │
                        │  │  3A: CHANNEL ADAPTER LAYER                 │    │
                        │  │  BankAdapter │ NfsAdapter │ ImpsAdapter    │    │
                        │  │  UpiAdapter  → canonical TransactionRequest│    │
                        │  └─────────────────────┬──────────────────────┘    │
                        │                        │                           │
                        │  ┌─────────────────────▼──────────────────────┐    │
                        │  │  3B: PRE-CHECK LAYER                       │    │
                        │  │  SanityChecker → BlacklistChecker          │    │
                        │  │  WhitelistChecker (fast-exit)              │    │
                        │  └─────────────────────┬──────────────────────┘    │
                        │                        │                           │
                        │  ┌─────────────────────▼──────────────────────┐    │
                        │  │  3C: RULE EXECUTION ENGINE                 │    │
                        │  │  HighAmountRule │ VelocityCountRule        │    │
                        │  │  MidnightTxnRule │ DormantAccountRule      │    │
                        │  │  GeoAnomalyRule │ AmountDeviationRule      │    │
                        │  │  … 25 rules total, independently evaluated │    │
                        │  └─────────────────────┬──────────────────────┘    │
                        │                        │                           │
                        │  ┌─────────────────────▼──────────────────────┐    │
                        │  │  3D: RISK SCORE AGGREGATOR                 │    │
                        │  │  Sum of triggered weights → 0-1000+ score  │    │
                        │  │  ALLOW(0-299) REVIEW(300-599) BLOCK(600+)  │    │
                        │  └─────────────────────┬──────────────────────┘    │
                        │                        │                           │
                        │         RESPONSE RETURNED SYNCHRONOUSLY ◄──────────┤
                        │                        │                           │
                        │  ┌─────────────────────▼──────────────────────┐    │
                        │  │  3E: POST-DECISION (ALL ASYNC)             │    │
                        │  │  AuditLogger │ AlertService                │    │
                        │  │  KafkaPublisher → fraud-events topic       │    │
                        │  └─────────────────────┬──────────────────────┘    │
                        │                        │                           │
                        │  ┌─────────────────────▼──────────────────────┐    │
                        │  │  3F: SUPPORTING SERVICES                   │    │
                        │  │  VelocityService │ BlacklistService        │    │
                        │  │  RuleConfigService │ CustomerProfileService │   │
                        │  └────────────────────────────────────────────┘    │
                        └─────────────────────────────────────────────────────┘
                                       │              │           │
              ┌────────────────────────┘              │           └──────────────┐
              │                                       │                          │
   ┌──────────▼──────────┐             ┌──────────────▼──────┐   ┌──────────────▼──────┐
   │     PostgreSQL       │             │       Redis          │   │    Apache Kafka      │
   │  11 tables           │             │  • Velocity ZSets    │   │  fraud-events topic  │
   │  Source of truth     │             │  • Blacklist cache   │   │  Consumer groups:    │
   │  Audit, Profiles     │             │  • Rule config cache │   │  • analytics         │
   │  Rules, Config       │             │  • Whitelist cache   │   │  • case-management   │
   └──────────────────────┘             └──────────────────────┘   │  • auto-blacklister  │
                                                                    │  • regulatory        │
                                                                    └──────────────────────┘
```

### Layer Descriptions

#### Layer 1: External Channel Sources

| Channel | Full Name | Protocol | Fraud Patterns |
|---------|-----------|----------|----------------|
| **BANK** | Core Banking NEFT/RTGS | ISO 20022 XML | Large wire fraud, structuring below CTR limits, dormant account reactivation |
| **NFS** | National Financial Switch | ISO 8583 | ATM skimming, card cloning, international withdrawal fraud, multi-ATM hop |
| **IMPS** | Immediate Payment Service | JSON REST | Phishing-driven fund transfers, account takeover, micro-transaction probing |
| **UPI** | Unified Payments Interface | UPI XML/JSON | Collect request spam, SIM swap, fake QR codes, peer pressure scams |

#### Layer 2: API Gateway

The API Gateway sits between external channels and the FRM Engine. Responsibilities:

- **Load Balancing** — distributes traffic across FRM Engine instances (Nginx upstream or Spring Cloud Gateway routes)
- **Rate Limiting** — per-IP and per-channel limits to prevent DDoS / brute-force
- **SSL Termination** — TLS 1.3 only; certificates rotated via Let's Encrypt / internal CA
- **Authentication** — mutual TLS (mTLS) for channel connections; JWT for internal service-to-service
- **Routing** — `/api/v1/fraud/check` routes to the FRM Engine; admin endpoints behind VPN
- **Health Checks** — liveness/readiness probes against `/actuator/health`

**Technology Choice:**
- **Nginx** — preferred for high-throughput raw performance (> 50k TPS)
- **Spring Cloud Gateway** — preferred when deep Spring ecosystem integration is needed (circuit breakers, OAuth2)

#### Layer 3A: Channel Adapter Layer

Each channel speaks a different "language". The Adapter pattern normalises all channel-specific payloads into a single canonical `TransactionRequest` DTO.

**Example — UPI XML vs IMPS JSON → same DTO:**

```
UPI XML payload:                    IMPS JSON payload:
<txnRefId>TXN001</txnRefId>         { "rrn": "TXN001",
<payerVpa>9876@upi</payerVpa>         "debitAccount": "ACC001",
<payeeVpa>merchant@upi</payeeVpa>     "creditAccount": "ACC002",
<amount>250000</amount>               "amount": 250000 }

                    ↓ both adapters produce ↓

TransactionRequest {
  transactionId = "TXN001"
  channel       = UPI / IMPS
  senderId      = "9876@upi" / "ACC001"
  receiverId    = "merchant@upi" / "ACC002"
  amount        = 250000
  currency      = "INR"
  txnTime       = LocalDateTime.now()
}
```

#### Layer 3B: Pre-Check Layer

Fast, early-exit checks before the full rule engine runs:

1. **SanityChecker** — validates: non-null/positive amount, non-blank sender/receiver, no self-transfer, non-null channel and txnTime. Returns an error string or null.
2. **BlacklistChecker** — queries Redis (L1 cache) → PostgreSQL (L2) for sender account, receiver account, device ID, and IP address.
3. **WhitelistChecker** — if sender is a trusted entity (government clearing house, own accounts), skip all rules and fast-exit with ALLOW.

**Fast-Exit Optimization:** If the blacklist score alone exceeds the BLOCK threshold (600+), the engine stops and returns BLOCK immediately without evaluating the 25 rules. This saves ~10-15ms per blocked transaction.

#### Layer 3C: Rule Execution Engine

- Rules are loaded from `frm_rule` table on startup (cached via Caffeine)
- Each rule is a Spring `@Component` implementing the `FraudRule` interface
- Rules are filtered by channel before evaluation (`applicable_channels` field)
- All rules evaluate independently in sequence (no short-circuit except blacklist)
- Each rule returns a `RuleResult` with `triggered: true/false`, `weight`, and `reason`
- New rules can be added by creating a new `@Component` class — **zero changes to the pipeline** (Open/Closed Principle)

#### Layer 3D: Risk Score Aggregator

```
Total Score = Σ(weight_i) for all triggered rules i

Score Range → Decision:
  0 – 299    → ALLOW
  300 – 599  → REVIEW
  600+       → BLOCK

Thresholds stored in frm_global_config (review_threshold, block_threshold)
Can be changed live via DB without restart.
```

#### Layer 3E: Post-Decision Actions (all @Async)

All post-decision actions are executed asynchronously on a separate thread pool so they do **not** add latency to the synchronous response:

- **AuditService** — persists `FrmAuditLog` record for every decision; auto-creates `FrmAlert` for REVIEW/BLOCK decisions
- **AlertService** (future) — sends email/SMS notifications to fraud analysts for REVIEW/BLOCK
- **FraudEventPublisher** — publishes `FraudEvent` to Kafka `fraud-events` topic for downstream consumers

#### Layer 3F: Supporting Services

| Service | Responsibility | Storage |
|---------|----------------|---------|
| `VelocityService` | Sliding-window transaction counts using Redis sorted sets | Redis |
| `BlacklistService` | Blacklist lookups with Caffeine in-process cache | Redis + PostgreSQL |
| `RuleConfigService` | Rule and parameter loading with Caffeine cache | PostgreSQL |
| `CustomerProfileService` | Running average amount, last city/device, txn count | PostgreSQL (async writes) |

#### Layer 4: External Data Stores

**PostgreSQL** — source of truth for all persistent data: 11 tables covering rules, config, audit logs, customer profiles, blacklists, alerts, device registry, beneficiary history.

**Redis** — low-latency access for:
- Velocity sorted sets: `vel:cnt:{senderId}` (sliding window transaction counts)
- Blacklist cache: `blacklist:{entityType}:{entityValue}` (TTL-based)
- Rule config cache: loaded at startup via Caffeine (in-process)

**Apache Kafka** — async event streaming:
- Topic: `fraud-events`
- Partitioned by `senderId` to maintain ordering per sender
- Consumer groups: `analytics` (BI dashboards), `case-management` (analyst workbench), `auto-blacklister` (ML-based auto-blacklist), `regulatory` (RBI/FIU reporting)

---

## 3. Complete Database Schema

```sql
-- 1. Channel master
CREATE TABLE frm_channel (
    channel_code  VARCHAR(10)  NOT NULL PRIMARY KEY,
    channel_name  VARCHAR(100) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. Fraud rules
CREATE TABLE frm_rule (
    rule_id              SERIAL       PRIMARY KEY,
    rule_code            VARCHAR(50)  NOT NULL UNIQUE,
    rule_name            VARCHAR(100) NOT NULL,
    description          TEXT,
    category             VARCHAR(30)  NOT NULL,
    risk_weight          INT          NOT NULL DEFAULT 0,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    applicable_channels  TEXT         NOT NULL DEFAULT 'ALL',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 3. Rule parameters (threshold values per rule)
CREATE TABLE frm_rule_parameter (
    param_id    SERIAL       PRIMARY KEY,
    rule_id     INT          NOT NULL REFERENCES frm_rule(rule_id) ON DELETE CASCADE,
    param_key   VARCHAR(50)  NOT NULL,
    param_value VARCHAR(200) NOT NULL,
    UNIQUE (rule_id, param_key)
);

-- 4. Global system configuration
CREATE TABLE frm_global_config (
    config_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description  TEXT,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 5. Blacklist
CREATE TABLE frm_blacklist (
    blacklist_id  BIGSERIAL    PRIMARY KEY,
    entity_type   VARCHAR(20)  NOT NULL,  -- ACCOUNT, DEVICE, IP
    entity_value  VARCHAR(200) NOT NULL,
    reason        TEXT,
    added_by      VARCHAR(100),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMP,
    UNIQUE (entity_type, entity_value)
);

-- 6. Whitelist (trusted entities — fast-exit ALLOW)
CREATE TABLE frm_whitelist (
    whitelist_id  BIGSERIAL    PRIMARY KEY,
    entity_type   VARCHAR(20)  NOT NULL,
    entity_value  VARCHAR(200) NOT NULL,
    reason        TEXT,
    added_by      VARCHAR(100),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMP,
    UNIQUE (entity_type, entity_value)
);

-- 7. Customer profile (behavioural baseline)
CREATE TABLE frm_customer_profile (
    customer_id         VARCHAR(50)    NOT NULL PRIMARY KEY,
    customer_name       VARCHAR(200),
    avg_txn_amount      NUMERIC(18,2)  NOT NULL DEFAULT 0,
    total_txn_count     BIGINT         NOT NULL DEFAULT 0,
    last_txn_date       TIMESTAMP,
    last_txn_city       VARCHAR(100),
    last_device_id      VARCHAR(200),
    account_open_date   DATE,
    risk_tier           VARCHAR(10)    NOT NULL DEFAULT 'LOW',  -- LOW/MEDIUM/HIGH
    known_beneficiaries TEXT,          -- comma-separated beneficiary IDs
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 8. Beneficiary history
CREATE TABLE frm_beneficiary_history (
    id             BIGSERIAL     PRIMARY KEY,
    sender_id      VARCHAR(50)   NOT NULL,
    beneficiary_id VARCHAR(50)   NOT NULL,
    first_txn_date TIMESTAMP     NOT NULL DEFAULT NOW(),
    txn_count      INT           NOT NULL DEFAULT 1,
    total_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    UNIQUE (sender_id, beneficiary_id)
);

-- 9. Audit log (every decision)
CREATE TABLE frm_audit_log (
    audit_id        BIGSERIAL     PRIMARY KEY,
    transaction_id  VARCHAR(100)  NOT NULL,
    channel         VARCHAR(10)   NOT NULL,
    sender_id       VARCHAR(50),
    receiver_id     VARCHAR(50),
    amount          NUMERIC(18,2),
    currency        VARCHAR(3)    NOT NULL DEFAULT 'INR',
    decision        VARCHAR(10)   NOT NULL,  -- ALLOW/REVIEW/BLOCK
    risk_score      INT           NOT NULL DEFAULT 0,
    triggered_rules JSONB,        -- array of {ruleCode, weight, reason}
    device_id       VARCHAR(200),
    sender_ip       VARCHAR(50),
    latitude        NUMERIC(10,6),
    longitude       NUMERIC(10,6),
    txn_time        TIMESTAMP,
    evaluation_ms   BIGINT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- 10. Alert (created for REVIEW/BLOCK decisions)
CREATE TABLE frm_alert (
    alert_id        BIGSERIAL    PRIMARY KEY,
    transaction_id  VARCHAR(100) NOT NULL,
    audit_id        BIGINT       NOT NULL REFERENCES frm_audit_log(audit_id),
    alert_status    VARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- OPEN/IN_REVIEW/CLOSED
    assigned_to     VARCHAR(100),
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',  -- LOW/MEDIUM/HIGH
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 11. Device registry
CREATE TABLE frm_device_registry (
    id          BIGSERIAL    PRIMARY KEY,
    device_id   VARCHAR(200) NOT NULL,
    customer_id VARCHAR(50)  NOT NULL,
    device_name VARCHAR(200),
    first_seen  TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_seen   TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_trusted  BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (device_id, customer_id)
);

-- Performance Indexes
CREATE INDEX idx_audit_txn_id       ON frm_audit_log (transaction_id);
CREATE INDEX idx_audit_sender       ON frm_audit_log (sender_id);
CREATE INDEX idx_audit_channel      ON frm_audit_log (channel);
CREATE INDEX idx_audit_decision     ON frm_audit_log (decision);
CREATE INDEX idx_audit_created      ON frm_audit_log (created_at);
CREATE INDEX idx_blacklist_entity   ON frm_blacklist (entity_type, entity_value) WHERE is_active = TRUE;
CREATE INDEX idx_whitelist_entity   ON frm_whitelist (entity_type, entity_value) WHERE is_active = TRUE;
CREATE INDEX idx_rule_active        ON frm_rule (is_active, applicable_channels);
CREATE INDEX idx_alert_status       ON frm_alert (alert_status);
CREATE INDEX idx_device_customer    ON frm_device_registry (customer_id);
CREATE INDEX idx_beneficiary_sender ON frm_beneficiary_history (sender_id);
CREATE INDEX idx_profile_risk_tier  ON frm_customer_profile (risk_tier);
```

---

## 4. Seed Data

See `src/main/resources/db/migration/V2__seed_initial_data.sql` for the full seed script containing:

- 4 channels: BANK, NFS, IMPS, UPI
- 5 global config entries (thresholds, currency, mode)
- 25 fraud rules with full descriptions
- All rule parameters

Key global config values at startup:

| Key | Value | Purpose |
|-----|-------|---------|
| `review_threshold` | 300 | Minimum score for REVIEW decision |
| `block_threshold` | 600 | Minimum score for BLOCK decision |
| `default_currency` | INR | Default transaction currency |
| `max_evaluation_time_ms` | 100 | SLA target for rule evaluation |
| `system_mode` | SHADOW | Start in log-only mode |

---

## 5. Spring Boot Project Structure

```
frm-engine/
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   └── FRM_ENGINE_COMPLETE_DOCUMENTATION.md
└── src/
    ├── main/
    │   ├── java/com/company/frm/
    │   │   ├── FrmEngineApplication.java
    │   │   ├── config/
    │   │   │   ├── AsyncConfig.java
    │   │   │   └── JacksonConfig.java
    │   │   ├── controller/
    │   │   │   └── FraudCheckController.java
    │   │   ├── dto/
    │   │   │   ├── TransactionRequest.java
    │   │   │   ├── FraudCheckResponse.java
    │   │   │   └── RuleResult.java
    │   │   ├── entity/
    │   │   │   ├── FrmChannel.java
    │   │   │   ├── FrmRule.java
    │   │   │   ├── FrmRuleParameter.java
    │   │   │   ├── FrmGlobalConfig.java
    │   │   │   ├── FrmBlacklist.java
    │   │   │   ├── FrmCustomerProfile.java
    │   │   │   ├── FrmBeneficiaryHistory.java
    │   │   │   ├── FrmAuditLog.java
    │   │   │   ├── FrmAlert.java
    │   │   │   └── FrmDeviceRegistry.java
    │   │   ├── enums/
    │   │   │   ├── Channel.java
    │   │   │   ├── Decision.java
    │   │   │   ├── SystemMode.java
    │   │   │   └── RiskTier.java
    │   │   ├── event/
    │   │   │   ├── FraudEvent.java
    │   │   │   └── FraudEventPublisher.java
    │   │   ├── engine/
    │   │   │   ├── FraudEvaluationPipeline.java
    │   │   │   ├── adapter/
    │   │   │   │   ├── ChannelAdapter.java
    │   │   │   │   ├── UpiAdapter.java
    │   │   │   │   └── ImpsAdapter.java
    │   │   │   ├── checker/
    │   │   │   │   ├── SanityChecker.java
    │   │   │   │   ├── BlacklistChecker.java
    │   │   │   │   └── WhitelistChecker.java
    │   │   │   ├── rule/
    │   │   │   │   ├── FraudRule.java
    │   │   │   │   ├── AbstractFraudRule.java
    │   │   │   │   ├── HighAmountRule.java
    │   │   │   │   ├── VelocityCountRule.java
    │   │   │   │   ├── MidnightTxnRule.java
    │   │   │   │   ├── DormantAccountRule.java
    │   │   │   │   ├── AmountDeviationRule.java
    │   │   │   │   └── GeoAnomalyRule.java
    │   │   │   └── scoring/
    │   │   │       └── RiskScoreAggregator.java
    │   │   ├── repository/
    │   │   │   ├── FrmRuleRepository.java
    │   │   │   ├── FrmRuleParameterRepository.java
    │   │   │   ├── FrmGlobalConfigRepository.java
    │   │   │   ├── FrmBlacklistRepository.java
    │   │   │   ├── FrmCustomerProfileRepository.java
    │   │   │   ├── FrmBeneficiaryHistoryRepository.java
    │   │   │   ├── FrmAuditLogRepository.java
    │   │   │   ├── FrmAlertRepository.java
    │   │   │   └── FrmDeviceRegistryRepository.java
    │   │   └── service/
    │   │       ├── RuleConfigService.java
    │   │       ├── BlacklistService.java
    │   │       ├── VelocityService.java
    │   │       ├── CustomerProfileService.java
    │   │       └── AuditService.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           ├── V1__init_frm_schema.sql
    │           └── V2__seed_initial_data.sql
    └── test/
        └── java/com/company/frm/
            └── FrmEngineTests.java
```

---

## 6. Complete Fraud Rules Catalog (25 Rules)

### Amount Rules

| # | Rule Code | Description | Weight | Channels |
|---|-----------|-------------|--------|----------|
| 1 | `HIGH_AMOUNT` | Single transaction exceeds 200,000 INR | 250 | ALL |
| 2 | `ROUND_AMOUNT` | Suspicious round amounts (50,000 / 100,000 / etc.) | 50 | ALL |
| 3 | `AMOUNT_BELOW_LIMIT` | Amount is 95–99% of regulatory CTR reporting limit (structuring) | 200 | ALL |
| 4 | `MICRO_TXN_PROBE` | Multiple tiny transactions (< ₹10) to test stolen credentials | 150 | UPI, IMPS |

### Velocity Rules

| # | Rule Code | Description | Weight | Channels |
|---|-----------|-------------|--------|----------|
| 5 | `VELOCITY_COUNT` | More than 5 transactions in any rolling 10-minute window | 300 | ALL |
| 6 | `VELOCITY_AMOUNT` | Cumulative amount > 500,000 INR in 10 minutes | 350 | ALL |
| 7 | `DAILY_COUNT_LIMIT` | More than 20 transactions in the current calendar day | 200 | ALL |
| 8 | `DAILY_AMOUNT_LIMIT` | Cumulative amount > 1,000,000 INR in the current day | 300 | ALL |
| 9 | `CROSS_CHANNEL_VELOCITY` | Same sender on 2+ channels within 30 minutes | 400 | ALL |

### Behavioral Rules

| # | Rule Code | Description | Weight | Channels |
|---|-----------|-------------|--------|----------|
| 10 | `MIDNIGHT_TXN` | Transaction between 00:00 and 05:00 | 100 | ALL |
| 11 | `DORMANT_ACCOUNT` | Account inactive for > 180 days | 300 | ALL |
| 12 | `FIRST_TIME_BENEFICIARY` | Amount >= ₹10,000 to a never-seen beneficiary | 200 | ALL |
| 13 | `RAPID_BENEFICIARY_ADD` | 3+ new beneficiaries added within 1 hour | 250 | ALL |
| 14 | `SPLIT_TXN_PATTERN` | 3+ transactions to same receiver within 30 minutes (structuring) | 350 | ALL |
| 15 | `AMOUNT_DEVIATION` | Transaction amount > 5× customer's historical average | 200 | ALL |

### Geo / Device Rules

| # | Rule Code | Description | Weight | Channels |
|---|-----------|-------------|--------|----------|
| 16 | `GEO_ANOMALY` | Impossible travel — different city within 60 minutes of last txn | 400 | ALL |
| 17 | `NEW_DEVICE` | Transaction from a device ID unknown to this customer | 150 | UPI, IMPS |
| 18 | `DEVICE_MULTI_ACCOUNT` | 1 device used by 3+ distinct customer accounts | 350 | UPI, IMPS |
| 19 | `SUSPICIOUS_IP` | Originating IP is a VPN / TOR / Proxy node | 300 | UPI, IMPS |

### List-Based Rules

| # | Rule Code | Description | Weight | Channels |
|---|-----------|-------------|--------|----------|
| 20 | `BLACKLISTED_SENDER` | Sender account is on the active blacklist — instant BLOCK | 1000 | ALL |
| 21 | `BLACKLISTED_BENEFICIARY` | Receiver account is on the active blacklist — instant BLOCK | 1000 | ALL |
| 22 | `BLACKLISTED_DEVICE` | Device ID is on the active blacklist | 800 | UPI, IMPS |

### Channel-Specific Rules

| # | Rule Code | Description | Weight | Channels |
|---|-----------|-------------|--------|----------|
| 23 | `NFS_INTL_ATM` | International ATM withdrawal (outside India) | 200 | NFS |
| 24 | `NFS_MULTI_ATM_HOP` | Same card used at 3+ ATMs within 1 hour | 350 | NFS |
| 25 | `UPI_COLLECT_SPAM` | Excessive UPI collect requests (> 5 in 10 min) | 150 | UPI |

---

## 7. Scoring Matrix and Decision Logic

### Score Scale

```
0         100       200       300       400       500       600       700       800+
|---------|---------|---------|---------|---------|---------|---------|---------|------
|                 ALLOW                |          REVIEW           |    BLOCK
|             (score 0–299)            |       (score 300–599)     |  (600+)
```

### Decision Boundaries

| Decision | Score Range | Action |
|----------|-------------|--------|
| **ALLOW** | 0 – 299 | Transaction proceeds normally |
| **REVIEW** | 300 – 599 | Transaction flagged; analyst reviews within SLA |
| **BLOCK** | 600+ | Transaction rejected; sender notified |

### Example Scenarios

**Scenario 1 — Normal Transaction**
```
Amount: ₹5,000 at 2 PM, known device, known beneficiary
Rules triggered: NONE
Total score: 0
Decision: ALLOW ✓
```

**Scenario 2 — Suspicious but not blocked**
```
Amount: ₹250,000 at 2:30 AM
HIGH_AMOUNT triggered  → +250
MIDNIGHT_TXN triggered → +100
Total score: 350
Decision: REVIEW ⚠
```

**Scenario 3 — Clear fraud signal**
```
Dormant account (2 years inactive), large amount, sudden velocity spike, geo anomaly
DORMANT_ACCOUNT     → +300
HIGH_AMOUNT         → +250
VELOCITY_COUNT      → +300
GEO_ANOMALY         → +400
Total score: 1250
Decision: BLOCK ✗
```

**Scenario 4 — Blacklist instant block**
```
Sender is on the active blacklist
BLACKLISTED_SENDER  → +1000
Fast-exit before rule engine runs
Decision: BLOCK ✗ (< 5ms total)
```

---

## 8. Complete Data Flow — End-to-End

### UPI Transaction: ₹250,000 at 2:30 AM

```
T+0ms   Customer initiates payment in UPI app
        → POST /api/v1/fraud/check to API Gateway

T+1ms   API Gateway: validates JWT, applies rate limit, routes to FRM Engine

T+2ms   UpiAdapter.adapt() converts UPI JSON payload to TransactionRequest DTO
        → transactionId, senderId (VPA), receiverId (VPA), amount=250000, txnTime=02:30

T+3ms   SanityChecker.check()
        → amount positive ✓, sender/receiver non-blank ✓, no self-transfer ✓
        → PASS

T+5ms   BlacklistChecker.check()
        → Redis lookup: sender VPA → NOT blacklisted
        → Redis lookup: receiver VPA → NOT blacklisted
        → Redis lookup: device ID → NOT blacklisted
        → PASS (no fast-exit)

T+6ms   WhitelistChecker.isSenderWhitelisted()
        → Redis lookup → NOT whitelisted
        → Continue to rule engine

T+8ms   HighAmountRule.evaluate()
        → 250000 > 200000 → TRIGGERED (+250)

T+9ms   MidnightTxnRule.evaluate()
        → hour=2, within [0,5) → TRIGGERED (+100)

T+10ms  VelocityCountRule.evaluate()
        → Redis: 2 txns in window → NOT triggered

T+11ms  DormantAccountRule.evaluate()
        → Profile: last_txn_date = 1 week ago → NOT triggered

T+12ms  AmountDeviationRule.evaluate()
        → Profile avg = 8000, threshold = 40000; 250000 > 40000 → TRIGGERED (+200)

T+13ms  GeoAnomalyRule.evaluate()
        → Same city as last txn → NOT triggered

T+15ms  Remaining rules evaluated (none triggered for this scenario)

T+20ms  RiskScoreAggregator.aggregate()
        → 250 + 100 + 200 = 550
        RiskScoreAggregator.decide()
        → 550 >= 300 (REVIEW threshold), 550 < 600 (BLOCK threshold)
        → Decision: REVIEW

T+21ms  FraudCheckResponse built and returned to caller:
        { decision: "REVIEW", riskScore: 550, triggeredRules: [...], evaluationMs: 21 }

T+22ms  [ASYNC] AuditService.logDecision() → INSERT into frm_audit_log
T+23ms  [ASYNC] AuditService.createAlert() → INSERT into frm_alert (priority: MEDIUM)
T+24ms  [ASYNC] CustomerProfileService.updateProfileAfterTxn() → UPDATE frm_customer_profile
T+25ms  [ASYNC] VelocityService (next call will count this txn in window)
T+27ms  [ASYNC] FraudEventPublisher.publish() → Kafka fraud-events topic
```

---

## 9. Tech Stack Summary

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Framework | Spring Boot | 3.2.3 | Application framework |
| Language | Java | 17 | Runtime |
| API | Spring Web (REST) | — | HTTP endpoints |
| Database | PostgreSQL | 15+ | Persistent store |
| ORM | Spring Data JPA / Hibernate | 6.x | DB access |
| Cache (L1) | Caffeine | 3.x | In-process rule/config cache |
| Cache (L2) | Redis (Lettuce) | 7.x | Velocity data, blacklist cache |
| Messaging | Apache Kafka | 3.x | Async fraud events |
| Migrations | Flyway | 9.x | Schema versioning |
| Monitoring | Micrometer + Prometheus | — | Metrics scraping |
| Dashboards | Grafana | — | Real-time fraud dashboards |
| Logging | SLF4J + Logback | — | Structured logging |
| Build | Maven | 3.8+ | Build and dependency management |
| Testing | JUnit 5 + Mockito | — | Unit tests |
| Integration Testing | Testcontainers | — | DB/Redis/Kafka integration tests |

---

## 10. Phased Rollout Plan

> **Critical for a new company** — the FRM engine starts in SHADOW mode and progresses through 4 phases. This prevents false positives from blocking legitimate customers while the system learns.

### Phase 1 — SHADOW MODE (Week 1–4)

**Configuration:** `system_mode = SHADOW`

| Action | Detail |
|--------|--------|
| All transactions | ALLOWED regardless of score |
| Engine behavior | Evaluates all 25 rules and logs results |
| Profiles | Customer behavioral profiles built organically |
| Velocity | Redis velocity baselines established |
| Analysts | Review audit logs daily to tune thresholds |
| Goal | Zero false positives; data collection only |

**Key metric to watch:** SHADOW-REVIEW rate (what % would have been reviewed). Target < 5%.

### Phase 2 — SOFT ENFORCEMENT (Week 5–8)

**Configuration:** `system_mode = ACTIVE`, limited rules enabled

| Action | Detail |
|--------|--------|
| BLOCK-only rules | BLACKLISTED_SENDER, BLACKLISTED_BENEFICIARY, SANITY_FAIL |
| REVIEW-only rules | HIGH_AMOUNT + VELOCITY (when combined score > 300) |
| All other rules | Still log-only |
| Monitor | False positive rate; analyst workload; customer complaints |
| Goal | < 0.1% false positive rate |

### Phase 3 — FULL ENFORCEMENT (Week 9–12)

**Configuration:** All 25 rules active

| Action | Detail |
|--------|--------|
| Enable progressively | Add 3–5 rules per week |
| Tune thresholds | Use Phase 1–2 data to adjust `param_value` in DB |
| Enable cross-channel | CROSS_CHANNEL_VELOCITY (requires data from all channels) |
| Alert queue | Fraud analyst alert queue fully operational |
| Goal | Stable false positive rate < 0.05%; capture rate > 85% |

### Phase 4 — OPTIMIZATION (Week 13+)

| Enhancement | Description |
|-------------|-------------|
| ML scoring | Add ML model score as an additional rule weight |
| Customer risk tiers | HIGH-tier customers get lower thresholds |
| Grafana dashboard | Real-time fraud score distribution, rule hit rates |
| Auto-blacklister | Kafka consumer auto-blacklists confirmed fraud accounts |
| Feedback loop | Analyst decisions feed back into rule weight tuning |

---

## 11. API Specification

### POST /api/v1/fraud/check

**Request:**
```json
{
  "transactionId": "UPI-20260309-023000-001",
  "channel": "UPI",
  "senderId": "9876543210@hdfc",
  "receiverId": "merchant-abc@paytm",
  "amount": 250000,
  "currency": "INR",
  "deviceId": "DEVICE-XYZ-123",
  "senderIp": "103.45.67.89",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "senderCity": "Mumbai",
  "txnTime": "2026-03-09T02:30:00",
  "upiVpa": "9876543210@hdfc",
  "collectRequest": false
}
```

**Response (BLOCK example):**
```json
{
  "transactionId": "UPI-20260309-023000-001",
  "decision": "BLOCK",
  "riskScore": 650,
  "triggeredRules": [
    {
      "ruleCode": "HIGH_AMOUNT",
      "ruleName": "High Transaction Amount",
      "triggered": true,
      "weight": 250,
      "reason": "Amount 250000 exceeds high-amount threshold of 200000"
    },
    {
      "ruleCode": "MIDNIGHT_TXN",
      "ruleName": "Midnight Hour Transaction",
      "triggered": true,
      "weight": 100,
      "reason": "Transaction at 2:00 is within midnight window (0:00-5:00)"
    },
    {
      "ruleCode": "AMOUNT_DEVIATION",
      "ruleName": "Unusual Amount Deviation",
      "triggered": true,
      "weight": 200,
      "reason": "Amount 250000 is >5x customer average of 8000"
    }
  ],
  "message": "Transaction blocked due to fraud risk",
  "evaluationMs": 21,
  "evaluatedAt": "2026-03-09T02:30:00.021",
  "shadowMode": false
}
```

### GET /api/v1/fraud/health

**Response:**
```
FRM Engine is running
```

---

## 12. Key Design Principles

### 1. Open/Closed Principle
New fraud rules are added by creating a new `@Component` class that implements `FraudRule` and is annotated with `@Component`. The `FraudEvaluationPipeline` auto-discovers all `FraudRule` beans via Spring's `List<FraudRule>` injection. **Zero changes to the pipeline or any existing class.**

### 2. Channel Agnostic Core
The FRM engine core never sees raw channel payloads. Each channel adapter converts its native format to the canonical `TransactionRequest` DTO. This means the rule engine works identically for BANK, NFS, IMPS, and UPI.

### 3. Externalized Thresholds
All thresholds (high_amount_threshold, velocity windows, dormant days) are stored in `frm_rule_parameter` and `frm_global_config`. Analysts can tune thresholds via a DB update — no code change, no restart required (cache TTL: 5 minutes).

### 4. Fail-Safe Design
- If Redis is unavailable: `VelocityCountRule` defaults to triggering (safe → REVIEW signal)
- If database is unavailable: cached rules continue to work until cache expires
- If Kafka publish fails: error is logged but does not affect the fraud check response

### 5. Idempotent Evaluation
The same `transactionId` evaluated twice produces the same result. The audit log may have two records, but the risk score and decision are deterministic for a given rule configuration and customer profile state.

### 6. Audit Everything
Every fraud check decision — ALLOW, REVIEW, or BLOCK — is persisted to `frm_audit_log` with full context (score, triggered rules, device, IP, coordinates). This satisfies:
- **RBI mandate** for transaction monitoring records (7-year retention)
- **PCI-DSS Requirement 10** for audit trails
- **Internal SLA** — analyst can reconstruct any decision in < 30 seconds

---

*End of Document — FRM Engine v1.0.0 — March 2026*
