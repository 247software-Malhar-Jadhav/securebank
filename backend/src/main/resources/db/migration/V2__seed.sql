-- =============================================================================
-- V2__seed.sql - demo / development seed data.
--
-- Passwords are BCrypt hashes of the literal "Password123!".
-- The hash below is a valid BCrypt $2a$ hash (strength 10) for that password,
-- so you can log in immediately after the migration runs:
--     admin / Password123!
--     jsmith / Password123!
--
-- (BCrypt salts are random, so this is ONE valid hash among many - Spring
-- Security's BCryptPasswordEncoder.matches() verifies it correctly.)
-- =============================================================================

-- ---- Users ----------------------------------------------------------------
INSERT INTO users (username, email, password_hash, role, enabled, preferred_locale)
VALUES
    ('admin',  'admin@securebank.local',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',    TRUE, 'en'),
    ('jsmith', 'jsmith@securebank.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER', TRUE, 'en');

-- ---- Customer profile for the demo customer (jsmith) ----------------------
INSERT INTO customers (user_id, first_name, last_name, phone, date_of_birth,
                       kyc_status, address_line, city, state, postal_code, country)
SELECT u.id, 'John', 'Smith', '+91-9000000000', DATE '1990-05-15',
       'VERIFIED', '12 MG Road', 'Pune', 'Maharashtra', '411001', 'India'
FROM users u WHERE u.username = 'jsmith';

-- ---- Two accounts for the demo customer -----------------------------------
INSERT INTO accounts (account_number, customer_id, type, currency, balance, status)
SELECT 'SB00000000000000000001', c.id, 'SAVINGS', 'INR', 50000.0000, 'ACTIVE'
FROM customers c JOIN users u ON c.user_id = u.id WHERE u.username = 'jsmith';

INSERT INTO accounts (account_number, customer_id, type, currency, balance, status)
SELECT 'SB00000000000000000002', c.id, 'CURRENT', 'INR', 12000.0000, 'ACTIVE'
FROM customers c JOIN users u ON c.user_id = u.id WHERE u.username = 'jsmith';

-- ---- A few seeded transactions on the SAVINGS account ---------------------
-- These are illustrative history rows. We also write matching ledger entries
-- so the double-entry view is consistent for the demo data.

-- An opening deposit of 50,000 (balance_after 50,000).
INSERT INTO transactions (reference, account_id, type, amount, currency, status, description, balance_after, fraud_score)
SELECT 'TXN-SEED-0000000001', a.id, 'DEPOSIT', 50000.0000, 'INR', 'COMPLETED', 'Opening deposit', 50000.0000, 0.0100
FROM accounts a WHERE a.account_number = 'SB00000000000000000001';

INSERT INTO ledger_entries (transaction_id, account_id, direction, amount)
SELECT t.id, a.id, 'CREDIT', 50000.0000
FROM transactions t JOIN accounts a ON a.id = t.account_id
WHERE t.reference = 'TXN-SEED-0000000001';

-- A grocery withdrawal of 1,500 (balance_after 48,500).
INSERT INTO transactions (reference, account_id, type, amount, currency, status, description, balance_after, fraud_score)
SELECT 'TXN-SEED-0000000002', a.id, 'WITHDRAWAL', 1500.0000, 'INR', 'COMPLETED', 'Groceries', 48500.0000, 0.0500
FROM accounts a WHERE a.account_number = 'SB00000000000000000001';

INSERT INTO ledger_entries (transaction_id, account_id, direction, amount)
SELECT t.id, a.id, 'DEBIT', 1500.0000
FROM transactions t JOIN accounts a ON a.id = t.account_id
WHERE t.reference = 'TXN-SEED-0000000002';

-- A utilities withdrawal of 2,000 (balance_after 46,500).
INSERT INTO transactions (reference, account_id, type, amount, currency, status, description, balance_after, fraud_score)
SELECT 'TXN-SEED-0000000003', a.id, 'WITHDRAWAL', 2000.0000, 'INR', 'COMPLETED', 'Electricity bill', 46500.0000, 0.0400
FROM accounts a WHERE a.account_number = 'SB00000000000000000001';

INSERT INTO ledger_entries (transaction_id, account_id, direction, amount)
SELECT t.id, a.id, 'DEBIT', 2000.0000
FROM transactions t JOIN accounts a ON a.id = t.account_id
WHERE t.reference = 'TXN-SEED-0000000003';

-- Reconcile the seeded SAVINGS balance with its ledger (50000 - 1500 - 2000).
UPDATE accounts SET balance = 46500.0000 WHERE account_number = 'SB00000000000000000001';

-- ---- A saved beneficiary for the demo customer ----------------------------
INSERT INTO beneficiaries (customer_id, name, account_number, bank_name)
SELECT c.id, 'Jane Doe', 'SB00000000000000000099', 'SecureBank'
FROM customers c JOIN users u ON c.user_id = u.id WHERE u.username = 'jsmith';
