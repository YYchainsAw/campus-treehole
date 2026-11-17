-- Run after 001 and 002 with application writes stopped. Preserve existing
-- positive author/liker IDs as placeholders so new accounts never reuse them.
CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wechat_appid VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    wechat_openid VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_wechat (wechat_appid, wechat_openid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT IGNORE INTO app_user (id) VALUES (1);
INSERT IGNORE INTO app_user (id) SELECT DISTINCT user_id FROM post WHERE user_id > 0;
INSERT IGNORE INTO app_user (id) SELECT DISTINCT user_id FROM post_like WHERE user_id > 0;

CREATE TABLE IF NOT EXISTS app_session (
    token_hash BINARY(32) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (token_hash),
    INDEX idx_session_expires_at (expires_at),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
