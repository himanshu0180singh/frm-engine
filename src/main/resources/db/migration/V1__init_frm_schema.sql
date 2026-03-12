-- ============================================================
-- FRM Engine - V1 Initial Schema
-- 11 tables for complete fraud risk management
-- ============================================================

-- --------------------------------------------------------
-- 1. frm_rule: Catalog of all fraud detection rules
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_rule (
    id               BIGSERIAL PRIMARY KEY,
    rule_code        VARCHAR(50)  NOT NULL UNIQUE,
    rule_name        VARCHAR(200) NOT NULL,
    description      TEXT,
    category         VARCHAR(50)  NOT NULL,
    base_score       INTEGER      NOT NULL DEFAULT 0,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_blocking      BOOLEAN      NOT NULL DEFAULT FALSE,
    priority         INTEGER      NOT NULL DEFAULT 100,
    applies_to       VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- --------------------------------------------------------
-- 2. frm_rule_parameter: Configurable parameters per rule
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_rule_parameter (
    id           BIGSERIAL PRIMARY KEY,
    rule_id      BIGINT      NOT NULL REFERENCES frm_rule(id) ON DELETE CASCADE,
    param_key    VARCHAR(100) NOT NULL,
    param_value  VARCHAR(500) NOT NULL,
    param_type   VARCHAR(50)  NOT NULL DEFAULT 'STRING',
    description  VARCHAR(500),
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (rule_id, param_key)
);

-- --------------------------------------------------------
-- 3. frm_customer_profile: Running risk profile per customer
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_customer_profile (
    id                      BIGSERIAL PRIMARY KEY,
    customer_id             VARCHAR(100) NOT NULL UNIQUE,
    risk_score              INTEGER      NOT NULL DEFAULT 0,
    risk_tier               VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    total_txn_count         BIGINT       NOT NULL DEFAULT 0,
    total_txn_amount        NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    avg_txn_amount          NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    last_txn_at             TIMESTAMP,
    last_txn_channel        VARCHAR(50),
    last_known_device_id    VARCHAR(200),
    last_known_ip           VARCHAR(50),
    fraud_flag_count        INTEGER      NOT NULL DEFAULT 0,
    is_dormant              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- --------------------------------------------------------
-- 4. frm_audit_log: Immutable audit trail for every check
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_audit_log (
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

CREATE INDEX IF NOT EXISTS idx_audit_log_txn_id      ON frm_audit_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_customer_id ON frm_audit_log(customer_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at  ON frm_audit_log(created_at);

-- --------------------------------------------------------
-- 5. frm_blacklist: Blacklisted entities (accounts, IPs, devices)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_blacklist (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_value VARCHAR(500) NOT NULL,
    reason       VARCHAR(500),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at   TIMESTAMP,
    created_by   VARCHAR(100),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (entity_type, entity_value)
);

CREATE INDEX IF NOT EXISTS idx_blacklist_entity ON frm_blacklist(entity_type, entity_value, is_active);

-- --------------------------------------------------------
-- 6. frm_whitelist: Whitelisted entities (trusted accounts)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_whitelist (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_value VARCHAR(500) NOT NULL,
    reason       VARCHAR(500),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at   TIMESTAMP,
    created_by   VARCHAR(100),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (entity_type, entity_value)
);

CREATE INDEX IF NOT EXISTS idx_whitelist_entity ON frm_whitelist(entity_type, entity_value, is_active);

-- --------------------------------------------------------
-- 7. frm_alert: Fraud alerts raised for REVIEW/BLOCK
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_alert (
    id               BIGSERIAL PRIMARY KEY,
    alert_ref        VARCHAR(100) NOT NULL UNIQUE,
    transaction_id   VARCHAR(100) NOT NULL,
    customer_id      VARCHAR(100) NOT NULL,
    channel          VARCHAR(50)  NOT NULL,
    amount           NUMERIC(20,2) NOT NULL,
    total_score      INTEGER      NOT NULL,
    final_decision   VARCHAR(20)  NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    analyst_id       VARCHAR(100),
    resolution_notes TEXT,
    resolved_at      TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alert_customer_id ON frm_alert(customer_id);
CREATE INDEX IF NOT EXISTS idx_alert_status      ON frm_alert(status);

-- --------------------------------------------------------
-- 8. frm_global_config: Runtime engine configuration
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_global_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(1000) NOT NULL,
    description  VARCHAR(500),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- --------------------------------------------------------
-- 9. frm_channel_config: Per-channel scoring overrides
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_channel_config (
    id                  BIGSERIAL PRIMARY KEY,
    channel             VARCHAR(50)  NOT NULL UNIQUE,
    allow_threshold     INTEGER      NOT NULL DEFAULT 299,
    review_threshold    INTEGER      NOT NULL DEFAULT 599,
    block_threshold     INTEGER      NOT NULL DEFAULT 600,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    max_daily_limit     NUMERIC(20,2),
    max_single_txn      NUMERIC(20,2),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- --------------------------------------------------------
-- 10. frm_rule_execution_stat: Metrics per rule for tuning
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_rule_execution_stat (
    id              BIGSERIAL PRIMARY KEY,
    rule_code       VARCHAR(50)  NOT NULL,
    stat_date       DATE         NOT NULL,
    total_fired     BIGINT       NOT NULL DEFAULT 0,
    total_blocked   BIGINT       NOT NULL DEFAULT 0,
    total_reviewed  BIGINT       NOT NULL DEFAULT 0,
    false_positives INTEGER      NOT NULL DEFAULT 0,
    avg_score       NUMERIC(10,2),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (rule_code, stat_date)
);

-- --------------------------------------------------------
-- 11. frm_case_note: Analyst notes on investigated cases
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS frm_case_note (
    id           BIGSERIAL PRIMARY KEY,
    alert_id     BIGINT       NOT NULL REFERENCES frm_alert(id) ON DELETE CASCADE,
    analyst_id   VARCHAR(100) NOT NULL,
    note_text    TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
