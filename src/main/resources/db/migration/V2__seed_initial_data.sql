-- ============================================================
-- FRM Engine - V2 Seed Initial Data
-- 25 Fraud Rules + Parameters + Channel Config + Global Config
-- ============================================================

-- --------------------------------------------------------
-- SECTION 1: Fraud Rules (25 rules)
-- --------------------------------------------------------
INSERT INTO frm_rule (rule_code, rule_name, description, category, base_score, is_active, is_blocking, priority, applies_to) VALUES
-- Velocity Rules
('VEL_COUNT_1H',   'High Transaction Count (1h)',       'More than N transactions in last 1 hour',                    'VELOCITY',     200, TRUE, FALSE, 10, 'UPI,IMPS,NFS,BANK'),
('VEL_AMOUNT_1H',  'High Volume Amount (1h)',            'Total transaction amount exceeds threshold in 1 hour',       'VELOCITY',     250, TRUE, FALSE, 10, 'UPI,IMPS,NFS,BANK'),
('VEL_COUNT_24H',  'High Transaction Count (24h)',       'More than N transactions in last 24 hours',                  'VELOCITY',     150, TRUE, FALSE, 15, 'UPI,IMPS,NFS,BANK'),
('VEL_NIGHT',      'Midnight Transaction Velocity',      'Multiple transactions between 00:00 and 04:00',              'VELOCITY',     180, TRUE, FALSE, 20, 'UPI,IMPS,NFS,BANK'),

-- Amount Rules
('AMT_HIGH',       'High Single Transaction Amount',     'Single transaction amount exceeds defined threshold',        'AMOUNT',       300, TRUE, FALSE,  5, 'UPI,IMPS,NFS,BANK'),
('AMT_DEVIATION',  'Amount Deviation from Avg',          'Amount deviates significantly from customer avg',            'AMOUNT',       220, TRUE, FALSE, 20, 'UPI,IMPS,NFS,BANK'),
('AMT_ROUND',      'Suspicious Round Amount',            'Transaction amount is an exact round number (potential test)','AMOUNT',       80, TRUE, FALSE, 50, 'UPI,IMPS'),
('AMT_JUST_BELOW', 'Amount Just Below Limit',            'Amount just below reporting limit (structuring signal)',     'AMOUNT',       180, TRUE, FALSE, 25, 'UPI,IMPS,NFS,BANK'),

-- Behavioral Rules
('BHDOT_DORMANT',  'Dormant Account Activity',           'Transaction from account dormant for over 90 days',          'BEHAVIORAL',   250, TRUE, FALSE, 10, 'UPI,IMPS,NFS,BANK'),
('BHDOT_NEWPAYEE', 'New Payee Large Amount',             'Large amount to a payee added less than 24h ago',            'BEHAVIORAL',   200, TRUE, FALSE, 15, 'UPI,IMPS'),
('BHDOT_FOREIGN',  'International Beneficiary',         'Beneficiary account registered in foreign country',           'BEHAVIORAL',   200, TRUE, FALSE, 20, 'IMPS,NFS,BANK'),
('BHDOT_CHANNEL_SWITCH', 'Unusual Channel Switch',      'Customer switched channel abruptly after failed attempt',     'BEHAVIORAL',   150, TRUE, FALSE, 30, 'UPI,IMPS,NFS,BANK'),

-- Device/Identity Rules
('DEV_NEW',        'New Device Detected',                'Transaction from a device not seen in last 30 days',         'DEVICE',       150, TRUE, FALSE, 20, 'UPI,IMPS'),
('DEV_MULTIPLE',   'Multiple Devices Same Session',      'More than 2 devices used within 1 hour',                     'DEVICE',       200, TRUE, FALSE, 15, 'UPI,IMPS'),
('DEV_ROOTED',     'Rooted/Jailbroken Device',           'Transaction from rooted or jailbroken device',               'DEVICE',       350, TRUE,  TRUE, 10, 'UPI'),

-- Geo Rules
('GEO_ANOMALY',    'Geographic Anomaly',                 'Transaction location far from last known location',          'GEO',          250, TRUE, FALSE, 10, 'UPI,IMPS'),
('GEO_BLOCKED_COUNTRY', 'Blocked Country IP',           'Request IP originates from a sanctioned/blocked country',    'GEO',          600, TRUE,  TRUE,  5, 'UPI,IMPS,NFS,BANK'),
('GEO_VPN',        'VPN/Proxy IP Detected',              'Request IP belongs to known VPN or proxy network',           'GEO',          200, TRUE, FALSE, 15, 'UPI,IMPS,NFS'),

-- Identity Rules
('ID_BLACKLIST',   'Blacklisted Entity',                 'Customer, account, IP, or device is blacklisted',            'IDENTITY',     700, TRUE,  TRUE,  1, 'UPI,IMPS,NFS,BANK'),
('ID_MISMATCH',    'Identity Mismatch',                  'Name/DOB mismatch between payer and registered profile',     'IDENTITY',     300, TRUE, FALSE,  5, 'NFS,BANK'),
('ID_MULTIPLE_FAIL','Multiple Auth Failures',            'More than 3 authentication failures in last 30 minutes',     'IDENTITY',     250, TRUE, FALSE, 10, 'UPI,IMPS,NFS,BANK'),

-- Network Rules
('NET_SHARED_DEVICE','Shared Device Multiple Accounts',  'Same device ID used by more than 3 accounts in 24h',         'NETWORK',      300, TRUE, FALSE,  5, 'UPI,IMPS'),
('NET_SHARED_IP',  'Shared IP Multiple Accounts',        'Same IP used by more than 5 accounts in 1 hour',             'NETWORK',      250, TRUE, FALSE, 10, 'UPI,IMPS,NFS,BANK'),

-- Merchant/Pattern Rules
('PAT_MULE',       'Mule Account Pattern',               'Account exhibits money mule behavior (receive then transfer)','PATTERN',      350, TRUE, FALSE,  5, 'UPI,IMPS,NFS,BANK'),
('PAT_BIN_ATTACK', 'BIN Attack Pattern',                 'Multiple small transactions testing card validity',          'PATTERN',      400, TRUE,  TRUE,  5, 'NFS,BANK')
;

-- --------------------------------------------------------
-- SECTION 2: Rule Parameters
-- --------------------------------------------------------
-- VEL_COUNT_1H
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_COUNT', '10', 'INTEGER', 'Max transactions allowed in 1 hour'
FROM frm_rule WHERE rule_code = 'VEL_COUNT_1H';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'WINDOW_SECONDS', '3600', 'INTEGER', 'Velocity window in seconds'
FROM frm_rule WHERE rule_code = 'VEL_COUNT_1H';

-- VEL_AMOUNT_1H
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_AMOUNT', '200000', 'DECIMAL', 'Max cumulative amount in 1 hour (INR)'
FROM frm_rule WHERE rule_code = 'VEL_AMOUNT_1H';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'WINDOW_SECONDS', '3600', 'INTEGER', 'Velocity window in seconds'
FROM frm_rule WHERE rule_code = 'VEL_AMOUNT_1H';

-- VEL_COUNT_24H
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_COUNT', '50', 'INTEGER', 'Max transactions allowed in 24 hours'
FROM frm_rule WHERE rule_code = 'VEL_COUNT_24H';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'WINDOW_SECONDS', '86400', 'INTEGER', 'Velocity window in seconds'
FROM frm_rule WHERE rule_code = 'VEL_COUNT_24H';

-- VEL_NIGHT
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'NIGHT_START_HOUR', '0', 'INTEGER', 'Midnight window start hour (0 = 12:00 AM)'
FROM frm_rule WHERE rule_code = 'VEL_NIGHT';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'NIGHT_END_HOUR', '4', 'INTEGER', 'Midnight window end hour (4 = 4:00 AM)'
FROM frm_rule WHERE rule_code = 'VEL_NIGHT';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_COUNT', '3', 'INTEGER', 'Max transactions during midnight window'
FROM frm_rule WHERE rule_code = 'VEL_NIGHT';

-- AMT_HIGH
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'THRESHOLD_AMOUNT', '500000', 'DECIMAL', 'Single transaction high amount threshold (INR)'
FROM frm_rule WHERE rule_code = 'AMT_HIGH';

-- AMT_DEVIATION
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'DEVIATION_MULTIPLIER', '5', 'DECIMAL', 'Score if amount > N * customer average'
FROM frm_rule WHERE rule_code = 'AMT_DEVIATION';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MIN_TXN_COUNT', '5', 'INTEGER', 'Minimum past transactions for deviation calc'
FROM frm_rule WHERE rule_code = 'AMT_DEVIATION';

-- AMT_JUST_BELOW
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'LIMIT_AMOUNT', '200000', 'DECIMAL', 'Reporting limit (structuring threshold INR)'
FROM frm_rule WHERE rule_code = 'AMT_JUST_BELOW';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'TOLERANCE_PCT', '5', 'DECIMAL', 'Percentage below limit to flag'
FROM frm_rule WHERE rule_code = 'AMT_JUST_BELOW';

-- BHDOT_DORMANT
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'DORMANT_DAYS', '90', 'INTEGER', 'Days of inactivity to consider dormant'
FROM frm_rule WHERE rule_code = 'BHDOT_DORMANT';

-- DEV_NEW
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'NEW_DEVICE_DAYS', '30', 'INTEGER', 'Days to consider a device as new'
FROM frm_rule WHERE rule_code = 'DEV_NEW';

-- DEV_MULTIPLE
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_DEVICES', '2', 'INTEGER', 'Max distinct devices in 1 hour'
FROM frm_rule WHERE rule_code = 'DEV_MULTIPLE';

-- GEO_ANOMALY
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_DISTANCE_KM', '500', 'DECIMAL', 'Max allowed distance from last known location (km)'
FROM frm_rule WHERE rule_code = 'GEO_ANOMALY';

-- ID_MULTIPLE_FAIL
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_FAILURES', '3', 'INTEGER', 'Max auth failures in window'
FROM frm_rule WHERE rule_code = 'ID_MULTIPLE_FAIL';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'WINDOW_MINUTES', '30', 'INTEGER', 'Auth failure check window in minutes'
FROM frm_rule WHERE rule_code = 'ID_MULTIPLE_FAIL';

-- NET_SHARED_DEVICE
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_ACCOUNTS', '3', 'INTEGER', 'Max accounts per device in 24h'
FROM frm_rule WHERE rule_code = 'NET_SHARED_DEVICE';

-- NET_SHARED_IP
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_ACCOUNTS', '5', 'INTEGER', 'Max accounts per IP in 1 hour'
FROM frm_rule WHERE rule_code = 'NET_SHARED_IP';

-- PAT_BIN_ATTACK
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'MAX_SMALL_TXN', '5', 'INTEGER', 'Max small test transactions in 10 minutes'
FROM frm_rule WHERE rule_code = 'PAT_BIN_ATTACK';
INSERT INTO frm_rule_parameter (rule_id, param_key, param_value, param_type, description)
SELECT id, 'SMALL_AMOUNT_LIMIT', '100', 'DECIMAL', 'Amount considered small for BIN attack (INR)'
FROM frm_rule WHERE rule_code = 'PAT_BIN_ATTACK';

-- --------------------------------------------------------
-- SECTION 3: Channel Configuration
-- --------------------------------------------------------
INSERT INTO frm_channel_config (channel, allow_threshold, review_threshold, block_threshold, is_active, max_daily_limit, max_single_txn) VALUES
('UPI',  299, 599, 600, TRUE, 100000.00,  100000.00),
('IMPS', 299, 599, 600, TRUE, 500000.00,  500000.00),
('NFS',  299, 599, 600, TRUE, 1000000.00, 500000.00),
('BANK', 299, 599, 600, TRUE, 2000000.00, 1000000.00)
;

-- --------------------------------------------------------
-- SECTION 4: Global Configuration
-- --------------------------------------------------------
INSERT INTO frm_global_config (config_key, config_value, description, is_active) VALUES
('ENGINE_ENABLED',              'true',        'Master switch to enable/disable FRM engine',       TRUE),
('DEFAULT_ALLOW_THRESHOLD',     '299',         'Default allow score threshold',                    TRUE),
('DEFAULT_REVIEW_THRESHOLD',    '599',         'Default review score threshold',                   TRUE),
('DEFAULT_BLOCK_THRESHOLD',     '600',         'Default block score threshold',                    TRUE),
('SHADOW_MODE_ENABLED',         'false',       'Shadow mode: evaluate but never block',            TRUE),
('ASYNC_AUDIT_ENABLED',         'true',        'Persist audit logs asynchronously',                TRUE),
('KAFKA_EVENTS_ENABLED',        'true',        'Publish fraud events to Kafka',                    TRUE),
('WHITELIST_BYPASS_ENABLED',    'true',        'Whitelisted entities bypass all rules',            TRUE),
('BLACKLIST_AUTO_BLOCK',        'true',        'Auto-block blacklisted entities immediately',      TRUE),
('VELOCITY_REDIS_ENABLED',      'true',        'Use Redis for velocity tracking',                  TRUE),
('MAX_RULES_CONCURRENT',        '25',          'Max concurrent rule executions per request',       TRUE),
('ALERT_AUTO_CREATE_THRESHOLD', '300',         'Min score to auto-create an alert',                TRUE),
('DORMANT_THRESHOLD_DAYS',      '90',          'Days of inactivity to mark account dormant',       TRUE),
('GEO_ANOMALY_KM_THRESHOLD',    '500',         'KM distance to flag geo anomaly',                  TRUE),
('ENGINE_VERSION',              '1.0.0',       'Current FRM engine version',                       TRUE)
;
