-- ============================================================
-- V1: FRM Engine — Complete Schema Initialization
-- ============================================================

-- 1. Channel master
CREATE TABLE IF NOT EXISTS frm_channel (
    channel_code  VARCHAR(10)  NOT NULL PRIMARY KEY,
    channel_name  VARCHAR(100) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. Fraud rules
CREATE TABLE IF NOT EXISTS frm_rule (
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

-- 3. Rule parameters
CREATE TABLE IF NOT EXISTS frm_rule_parameter (
    param_id    SERIAL       PRIMARY KEY,
    rule_id     INT          NOT NULL REFERENCES frm_rule(rule_id) ON DELETE CASCADE,
    param_key   VARCHAR(50)  NOT NULL,
    param_value VARCHAR(200) NOT NULL,
    UNIQUE (rule_id, param_key)
);

-- 4. Global configuration
CREATE TABLE IF NOT EXISTS frm_global_config (
    config_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description  TEXT,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 5. Blacklist
CREATE TABLE IF NOT EXISTS frm_blacklist (
    blacklist_id  BIGSERIAL    PRIMARY KEY,
    entity_type   VARCHAR(20)  NOT NULL,
    entity_value  VARCHAR(200) NOT NULL,
    reason        TEXT,
    added_by      VARCHAR(100),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMP,
    UNIQUE (entity_type, entity_value)
);

-- 6. Whitelist
CREATE TABLE IF NOT EXISTS frm_whitelist (
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

-- 7. Customer profile
CREATE TABLE IF NOT EXISTS frm_customer_profile (
    customer_id         VARCHAR(50)    NOT NULL PRIMARY KEY,
    customer_name       VARCHAR(200),
    avg_txn_amount      NUMERIC(18,2)  NOT NULL DEFAULT 0,
    total_txn_count     BIGINT         NOT NULL DEFAULT 0,
    last_txn_date       TIMESTAMP,
    last_txn_city       VARCHAR(100),
    last_device_id      VARCHAR(200),
    account_open_date   DATE,
    risk_tier           VARCHAR(10)    NOT NULL DEFAULT 'LOW',
    known_beneficiaries TEXT,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 8. Beneficiary history
CREATE TABLE IF NOT EXISTS frm_beneficiary_history (
    id             BIGSERIAL     PRIMARY KEY,
    sender_id      VARCHAR(50)   NOT NULL,
    beneficiary_id VARCHAR(50)   NOT NULL,
    first_txn_date TIMESTAMP     NOT NULL DEFAULT NOW(),
    txn_count      INT           NOT NULL DEFAULT 1,
    total_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    UNIQUE (sender_id, beneficiary_id)
);

-- 9. Audit log
CREATE TABLE IF NOT EXISTS frm_audit_log (
    audit_id        BIGSERIAL     PRIMARY KEY,
    transaction_id  VARCHAR(100)  NOT NULL,
    channel         VARCHAR(10)   NOT NULL,
    sender_id       VARCHAR(50),
    receiver_id     VARCHAR(50),
    amount          NUMERIC(18,2),
    currency        VARCHAR(3)    NOT NULL DEFAULT 'INR',
    decision        VARCHAR(10)   NOT NULL,
    risk_score      INT           NOT NULL DEFAULT 0,
    triggered_rules JSONB,
    device_id       VARCHAR(200),
    sender_ip       VARCHAR(50),
    latitude        NUMERIC(10,6),
    longitude       NUMERIC(10,6),
    txn_time        TIMESTAMP,
    evaluation_ms   BIGINT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- 10. Alert
CREATE TABLE IF NOT EXISTS frm_alert (
    alert_id        BIGSERIAL    PRIMARY KEY,
    transaction_id  VARCHAR(100) NOT NULL,
    audit_id        BIGINT       NOT NULL REFERENCES frm_audit_log(audit_id),
    alert_status    VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_to     VARCHAR(100),
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 11. Device registry
CREATE TABLE IF NOT EXISTS frm_device_registry (
    id          BIGSERIAL    PRIMARY KEY,
    device_id   VARCHAR(200) NOT NULL,
    customer_id VARCHAR(50)  NOT NULL,
    device_name VARCHAR(200),
    first_seen  TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_seen   TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_trusted  BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (device_id, customer_id)
);

-- ============================================================
-- Performance Indexes
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_audit_txn_id        ON frm_audit_log (transaction_id);
CREATE INDEX IF NOT EXISTS idx_audit_sender        ON frm_audit_log (sender_id);
CREATE INDEX IF NOT EXISTS idx_audit_channel       ON frm_audit_log (channel);
CREATE INDEX IF NOT EXISTS idx_audit_decision      ON frm_audit_log (decision);
CREATE INDEX IF NOT EXISTS idx_audit_created       ON frm_audit_log (created_at);
CREATE INDEX IF NOT EXISTS idx_blacklist_entity    ON frm_blacklist (entity_type, entity_value) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_whitelist_entity    ON frm_whitelist (entity_type, entity_value) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_rule_active         ON frm_rule (is_active, applicable_channels);
CREATE INDEX IF NOT EXISTS idx_alert_status        ON frm_alert (alert_status);
CREATE INDEX IF NOT EXISTS idx_device_customer     ON frm_device_registry (customer_id);
CREATE INDEX IF NOT EXISTS idx_beneficiary_sender  ON frm_beneficiary_history (sender_id);
CREATE INDEX IF NOT EXISTS idx_profile_risk_tier   ON frm_customer_profile (risk_tier);
