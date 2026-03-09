-- ============================================================
-- V2: FRM Engine — Seed Initial Data
-- ============================================================

-- ============================================================
-- Channels
-- ============================================================
INSERT INTO frm_channel (channel_code, channel_name, is_active) VALUES
    ('BANK',  'Core Banking (NEFT/RTGS)',        TRUE),
    ('NFS',   'National Financial Switch (ATM)',  TRUE),
    ('IMPS',  'Immediate Payment Service',        TRUE),
    ('UPI',   'Unified Payments Interface',       TRUE)
ON CONFLICT (channel_code) DO NOTHING;

-- ============================================================
-- Global Configuration
-- ============================================================
INSERT INTO frm_global_config (config_key, config_value, description) VALUES
    ('review_threshold',        '300',    'Minimum score to set decision to REVIEW'),
    ('block_threshold',         '600',    'Minimum score to set decision to BLOCK'),
    ('default_currency',        'INR',    'Default transaction currency'),
    ('max_evaluation_time_ms',  '100',    'Maximum allowed rule evaluation time in milliseconds'),
    ('system_mode',             'SHADOW', 'SHADOW: log-only mode; ACTIVE: enforcement enabled')
ON CONFLICT (config_key) DO NOTHING;

-- ============================================================
-- Fraud Rules (25 rules)
-- ============================================================

-- AMOUNT RULES
INSERT INTO frm_rule (rule_code, rule_name, description, category, risk_weight, is_active, applicable_channels) VALUES
('HIGH_AMOUNT',
 'High Transaction Amount',
 'Single transaction amount exceeds the high-amount threshold (default 200,000 INR). Large one-off transfers are a common fraud vector.',
 'AMOUNT', 250, TRUE, 'ALL'),

('ROUND_AMOUNT',
 'Suspicious Round Amount',
 'Transaction amount is a suspiciously round number (e.g. 50000, 100000). Fraudsters often use round amounts to test stolen credentials.',
 'AMOUNT', 50, TRUE, 'ALL'),

('AMOUNT_BELOW_LIMIT',
 'Amount Just Below Reporting Limit',
 'Transaction amount is between 95% and 99% of the regulatory cash reporting limit, indicating possible structuring to avoid CTR filing.',
 'AMOUNT', 200, TRUE, 'ALL'),

('MICRO_TXN_PROBE',
 'Micro Transaction Probing',
 'Multiple tiny transactions (< 10 INR each) in quick succession, used to verify stolen credentials before large withdrawal.',
 'AMOUNT', 150, TRUE, 'UPI,IMPS');

-- VELOCITY RULES
INSERT INTO frm_rule (rule_code, rule_name, description, category, risk_weight, is_active, applicable_channels) VALUES
('VELOCITY_COUNT',
 'High Transaction Count Velocity',
 'Sender initiates more than 5 transactions within any rolling 10-minute window. Strong indicator of automated fraud.',
 'VELOCITY', 300, TRUE, 'ALL'),

('VELOCITY_AMOUNT',
 'High Amount Velocity',
 'Cumulative transaction amount from the same sender exceeds 500,000 INR within a rolling 10-minute window.',
 'VELOCITY', 350, TRUE, 'ALL'),

('DAILY_COUNT_LIMIT',
 'Daily Transaction Count Limit',
 'Sender has exceeded 20 transactions in the current calendar day, suggesting account compromise or mule activity.',
 'VELOCITY', 200, TRUE, 'ALL'),

('DAILY_AMOUNT_LIMIT',
 'Daily Amount Limit',
 'Cumulative amount sent exceeds 1,000,000 INR in the current calendar day.',
 'VELOCITY', 300, TRUE, 'ALL'),

('CROSS_CHANNEL_VELOCITY',
 'Cross-Channel Velocity',
 'Same sender account has been active on 2 or more different channels (e.g. UPI + NFS) within a 30-minute window, which is unusual.',
 'VELOCITY', 400, TRUE, 'ALL');

-- BEHAVIORAL RULES
INSERT INTO frm_rule (rule_code, rule_name, description, category, risk_weight, is_active, applicable_channels) VALUES
('MIDNIGHT_TXN',
 'Midnight Hour Transaction',
 'Transaction initiated between 00:00 and 05:00 local time. Late-night transactions have a higher fraud base rate.',
 'BEHAVIORAL', 100, TRUE, 'ALL'),

('DORMANT_ACCOUNT',
 'Dormant Account Activity',
 'Account has had no transaction activity for more than 180 days and is now initiating a transaction. Classic account takeover signal.',
 'BEHAVIORAL', 300, TRUE, 'ALL'),

('FIRST_TIME_BENEFICIARY',
 'Large Amount to New Beneficiary',
 'Sender is transferring a significant amount (>= 10,000 INR) to a beneficiary they have never transacted with before.',
 'BEHAVIORAL', 200, TRUE, 'ALL'),

('RAPID_BENEFICIARY_ADD',
 'Rapid New Beneficiary Addition',
 'Sender has added 3 or more new beneficiaries within the past 1 hour, indicating account takeover with beneficiary harvesting.',
 'BEHAVIORAL', 250, TRUE, 'ALL'),

('SPLIT_TXN_PATTERN',
 'Split Transaction / Structuring Pattern',
 'Multiple transactions sent to the same receiver within a 30-minute window, suggesting deliberate structuring to avoid detection thresholds.',
 'BEHAVIORAL', 350, TRUE, 'ALL'),

('AMOUNT_DEVIATION',
 'Unusual Amount Deviation',
 'Transaction amount is more than 5x the customer''s historical average transaction amount, indicating anomalous behaviour.',
 'BEHAVIORAL', 200, TRUE, 'ALL');

-- GEO / DEVICE RULES
INSERT INTO frm_rule (rule_code, rule_name, description, category, risk_weight, is_active, applicable_channels) VALUES
('GEO_ANOMALY',
 'Geographical Anomaly / Impossible Travel',
 'Two transactions from the same account in locations more than 500 km apart within 1 hour — physically impossible travel.',
 'GEO', 400, TRUE, 'ALL'),

('NEW_DEVICE',
 'Unknown Device',
 'Transaction originated from a device ID not previously registered for this customer.',
 'GEO', 150, TRUE, 'UPI,IMPS'),

('DEVICE_MULTI_ACCOUNT',
 'Device Used by Multiple Accounts',
 'A single device ID has been used by 3 or more distinct customer accounts, indicating a device shared by a fraud ring.',
 'GEO', 350, TRUE, 'UPI,IMPS'),

('SUSPICIOUS_IP',
 'Suspicious IP Address',
 'Transaction originated from an IP address identified as a VPN exit node, TOR exit node, or known proxy server.',
 'GEO', 300, TRUE, 'UPI,IMPS');

-- LIST-BASED RULES
INSERT INTO frm_rule (rule_code, rule_name, description, category, risk_weight, is_active, applicable_channels) VALUES
('BLACKLISTED_SENDER',
 'Blacklisted Sender',
 'The sender account is present and active on the FRM blacklist. Triggers an immediate BLOCK regardless of other scores.',
 'LIST', 1000, TRUE, 'ALL'),

('BLACKLISTED_BENEFICIARY',
 'Blacklisted Beneficiary',
 'The receiver account is present and active on the FRM blacklist. Triggers an immediate BLOCK regardless of other scores.',
 'LIST', 1000, TRUE, 'ALL'),

('BLACKLISTED_DEVICE',
 'Blacklisted Device',
 'The originating device ID is present and active on the FRM blacklist. High-confidence fraud signal.',
 'LIST', 800, TRUE, 'UPI,IMPS');

-- CHANNEL-SPECIFIC RULES
INSERT INTO frm_rule (rule_code, rule_name, description, category, risk_weight, is_active, applicable_channels) VALUES
('NFS_INTL_ATM',
 'International ATM Withdrawal',
 'ATM transaction originating outside India. Valid only on the NFS channel. Customer should be notified immediately.',
 'CHANNEL', 200, TRUE, 'NFS'),

('NFS_MULTI_ATM_HOP',
 'Multiple ATM Hops (NFS)',
 'Same card / account used at 3 or more different ATMs within a 1-hour window on the NFS channel.',
 'CHANNEL', 350, TRUE, 'NFS'),

('UPI_COLLECT_SPAM',
 'UPI Collect Request Spam',
 'A single UPI VPA has sent excessive collect (pull) requests in a short period, a common social-engineering phishing technique.',
 'CHANNEL', 150, TRUE, 'UPI');

-- ============================================================
-- Rule Parameters
-- ============================================================
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'high_amount_threshold', '200000' FROM frm_rule WHERE rule_code = 'HIGH_AMOUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'round_amount_values', '10000,25000,50000,75000,100000,150000,200000,500000,1000000' FROM frm_rule WHERE rule_code = 'ROUND_AMOUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'reporting_limit', '1000000' FROM frm_rule WHERE rule_code = 'AMOUNT_BELOW_LIMIT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'micro_amount_threshold', '10' FROM frm_rule WHERE rule_code = 'MICRO_TXN_PROBE'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'min_probe_count', '3' FROM frm_rule WHERE rule_code = 'MICRO_TXN_PROBE'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_count', '5' FROM frm_rule WHERE rule_code = 'VELOCITY_COUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '10' FROM frm_rule WHERE rule_code = 'VELOCITY_COUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_amount', '500000' FROM frm_rule WHERE rule_code = 'VELOCITY_AMOUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '10' FROM frm_rule WHERE rule_code = 'VELOCITY_AMOUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_daily_count', '20' FROM frm_rule WHERE rule_code = 'DAILY_COUNT_LIMIT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_daily_amount', '1000000' FROM frm_rule WHERE rule_code = 'DAILY_AMOUNT_LIMIT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '30' FROM frm_rule WHERE rule_code = 'CROSS_CHANNEL_VELOCITY'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'min_channels', '2' FROM frm_rule WHERE rule_code = 'CROSS_CHANNEL_VELOCITY'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'start_hour', '0' FROM frm_rule WHERE rule_code = 'MIDNIGHT_TXN'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'end_hour', '5' FROM frm_rule WHERE rule_code = 'MIDNIGHT_TXN'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'dormant_days', '180' FROM frm_rule WHERE rule_code = 'DORMANT_ACCOUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'min_amount', '10000' FROM frm_rule WHERE rule_code = 'FIRST_TIME_BENEFICIARY'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'min_new_beneficiaries', '3' FROM frm_rule WHERE rule_code = 'RAPID_BENEFICIARY_ADD'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '60' FROM frm_rule WHERE rule_code = 'RAPID_BENEFICIARY_ADD'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'min_txn_count', '3' FROM frm_rule WHERE rule_code = 'SPLIT_TXN_PATTERN'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '30' FROM frm_rule WHERE rule_code = 'SPLIT_TXN_PATTERN'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'deviation_multiplier', '5' FROM frm_rule WHERE rule_code = 'AMOUNT_DEVIATION'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_km_per_hour', '500' FROM frm_rule WHERE rule_code = 'GEO_ANOMALY'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_device_accounts', '3' FROM frm_rule WHERE rule_code = 'DEVICE_MULTI_ACCOUNT'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_atm_count', '3' FROM frm_rule WHERE rule_code = 'NFS_MULTI_ATM_HOP'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '60' FROM frm_rule WHERE rule_code = 'NFS_MULTI_ATM_HOP'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'max_collect_requests', '5' FROM frm_rule WHERE rule_code = 'UPI_COLLECT_SPAM'
ON CONFLICT (rule_id, param_key) DO NOTHING;

INSERT INTO frm_rule_parameter (rule_id, param_key, param_value)
SELECT rule_id, 'window_minutes', '10' FROM frm_rule WHERE rule_code = 'UPI_COLLECT_SPAM'
ON CONFLICT (rule_id, param_key) DO NOTHING;
