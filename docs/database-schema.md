# WealthCopilot — Database Schema (MySQL 8)

Managed by Flyway migrations in `backend/src/main/resources/db/migration/`.
All tables InnoDB, `utf8mb4`. Money/quantity columns use `DECIMAL` (never
FLOAT). Every user-owned table has a `user_id` FK — the enforcement point for
per-user data isolation.

## Entity relationship overview

```
users 1──* transactions *──1 instruments 1──1 price_cache
users 1──* conversations 1──* chat_messages
api_keys (standalone, instructor access)
```

---

## Tables

### users

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(100) | NOT NULL (BCrypt) |
| display_name | VARCHAR(100) | NOT NULL |
| created_at | DATETIME(3) | NOT NULL, default CURRENT_TIMESTAMP |
| updated_at | DATETIME(3) | NOT NULL, on update CURRENT_TIMESTAMP |

### instruments
Shared reference data (not user-owned). Populated lazily from Twelve Data
symbol search when a user first references a ticker.

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| ticker | VARCHAR(20) | NOT NULL |
| exchange | VARCHAR(20) | NULL |
| name | VARCHAR(255) | NULL |
| type | ENUM('STOCK','ETF') | NOT NULL |
| currency | CHAR(3) | NOT NULL, default 'USD' |
| created_at | DATETIME(3) | NOT NULL |
|  |  | UNIQUE KEY uq_instrument (ticker, exchange) |

### transactions

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, FK → users.id |
| instrument_id | BIGINT | NOT NULL, FK → instruments.id |
| side | ENUM('BUY','SELL') | NOT NULL |
| quantity | DECIMAL(18,6) | NOT NULL, CHECK (quantity > 0) |
| price | DECIMAL(18,4) | NOT NULL, CHECK (price >= 0) — per-share, instrument currency |
| fees | DECIMAL(18,4) | NOT NULL, default 0 |
| trade_date | DATE | NOT NULL |
| note | VARCHAR(500) | NULL |
| source | ENUM('MANUAL','AI_ASSISTED') | NOT NULL, default 'MANUAL' — audit whether NL entry was used |
| created_at | DATETIME(3) | NOT NULL |
| updated_at | DATETIME(3) | NOT NULL |
|  |  | KEY idx_tx_user_date (user_id, trade_date) |
|  |  | KEY idx_tx_user_instrument (user_id, instrument_id) |

Holdings are **derived** from transactions (average-cost basis), not stored.
The service layer re-validates the instrument's full timeline on **every**
write (create, update, delete): if the position would go negative at any
trade date, the write is rejected. v1 accepts USD instruments only.

### price_cache
One row per instrument — the latest quote. History is out of scope for v1
(add `price_history(instrument_id, date, close)` later for charts).

| Column | Type | Constraints |
|---|---|---|
| instrument_id | BIGINT | PK, FK → instruments.id |
| price | DECIMAL(18,4) | NOT NULL |
| previous_close | DECIMAL(18,4) | NULL |
| as_of | DATETIME(3) | NOT NULL — quote timestamp from provider |
| fetched_at | DATETIME(3) | NOT NULL — when we fetched it |

### conversations

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, FK → users.id |
| title | VARCHAR(200) | NULL (auto-generated from first message) |
| created_at | DATETIME(3) | NOT NULL |
| updated_at | DATETIME(3) | NOT NULL |
|  |  | KEY idx_conv_user (user_id, updated_at) |

### chat_messages

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| conversation_id | BIGINT | NOT NULL, FK → conversations.id ON DELETE CASCADE |
| role | ENUM('USER','ASSISTANT','TOOL') | NOT NULL |
| content | TEXT | NOT NULL |
| tool_name | VARCHAR(50) | NULL — set when role='TOOL' |
| created_at | DATETIME(3) | NOT NULL |
|  |  | KEY idx_msg_conv (conversation_id, created_at) |

User scoping is transitive via `conversation_id` → `conversations.user_id`;
repository queries join through conversations with the user id.

### api_keys
Instructor / external read-only access.

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| key_hash | CHAR(64) | NOT NULL, UNIQUE — SHA-256 of the key; plaintext shown once |
| label | VARCHAR(100) | NOT NULL (e.g. "Instructor demo key") |
| scope | ENUM('READ_ONLY') | NOT NULL, default 'READ_ONLY' |
| created_at | DATETIME(3) | NOT NULL |
| last_used_at | DATETIME(3) | NULL |
| revoked_at | DATETIME(3) | NULL — non-null = revoked |

---

## Migration plan

| Migration | Contents |
|---|---|
| V1__baseline.sql | users, instruments, transactions, price_cache |
| V2__ai_chat.sql | conversations, chat_messages |
| V3__external_api.sql | api_keys |
