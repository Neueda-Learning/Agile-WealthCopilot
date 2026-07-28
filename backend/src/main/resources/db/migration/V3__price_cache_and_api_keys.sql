CREATE TABLE IF NOT EXISTS price_cache (
    instrument_id BIGINT NOT NULL,
    price DECIMAL(18, 4) NOT NULL,
    previous_close DECIMAL(18, 4) NULL,
    as_of DATETIME(3) NOT NULL,
    fetched_at DATETIME(3) NOT NULL,
    PRIMARY KEY (instrument_id),
    CONSTRAINT fk_price_cache_instrument FOREIGN KEY (instrument_id) REFERENCES instruments (id),
    CONSTRAINT ck_price_cache_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_price_cache_previous_close_non_negative CHECK (previous_close IS NULL OR previous_close >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    key_hash CHAR(64) NOT NULL,
    label VARCHAR(100) NOT NULL,
    scope ENUM('READ_ONLY') NOT NULL DEFAULT 'READ_ONLY',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_used_at DATETIME(3) NULL,
    revoked_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_api_key_hash (key_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
