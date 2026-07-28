CREATE TABLE IF NOT EXISTS instruments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticker VARCHAR(20) NOT NULL,
    exchange VARCHAR(20) NULL,
    name VARCHAR(255) NULL,
    type ENUM('STOCK', 'ETF') NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_instrument (ticker, exchange)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    side ENUM('BUY', 'SELL') NOT NULL,
    quantity DECIMAL(18, 6) NOT NULL,
    price DECIMAL(18, 4) NOT NULL,
    fees DECIMAL(18, 4) NOT NULL DEFAULT 0,
    trade_date DATE NOT NULL,
    note VARCHAR(500) NULL,
    source ENUM('MANUAL', 'AI_ASSISTED') NOT NULL DEFAULT 'MANUAL',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_tx_user_date (user_id, trade_date),
    KEY idx_tx_user_instrument (user_id, instrument_id),
    CONSTRAINT fk_transactions_instrument FOREIGN KEY (instrument_id) REFERENCES instruments (id),
    CONSTRAINT ck_tx_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_tx_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_tx_fees_non_negative CHECK (fees >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
