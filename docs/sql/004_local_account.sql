-- Apply after 003. Local demonstration accounts have independent app_user IDs.
CREATE TABLE IF NOT EXISTS local_account (
    user_id BIGINT NOT NULL,
    username VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    password_salt BINARY(16) NOT NULL,
    password_hash BINARY(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT uk_local_account_username UNIQUE (username),
    CONSTRAINT fk_local_account_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
