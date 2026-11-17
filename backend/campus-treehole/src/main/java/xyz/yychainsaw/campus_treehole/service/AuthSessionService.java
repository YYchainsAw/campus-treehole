package xyz.yychainsaw.campus_treehole.service;

import xyz.yychainsaw.campus_treehole.util.TokenSupport;
import xyz.yychainsaw.campus_treehole.dto.LoginResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthSessionService {
    private final JdbcTemplate jdbc;

    public AuthSessionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public LoginResponse createSession(WechatClient.WechatIdentity identity) {
        jdbc.update("INSERT INTO app_user (wechat_appid, wechat_openid) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE id = id", identity.appId(), identity.openId());
        Long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE wechat_appid = ? AND wechat_openid = ?",
                Long.class, identity.appId(), identity.openId());
        return createSessionForUser(userId);
    }

    @Transactional
    public LoginResponse createSessionForUser(long userId) {
        String token = TokenSupport.newToken();
        byte[] tokenHash = TokenSupport.hash(token);
        jdbc.update("INSERT INTO app_session (token_hash, user_id, expires_at) "
                + "VALUES (?, ?, DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 7 DAY))", tokenHash, userId);
        String expiresAt = jdbc.queryForObject("SELECT DATE_FORMAT(expires_at, '%Y-%m-%dT%H:%i:%s.%fZ') "
                + "FROM app_session WHERE token_hash = ?", String.class, tokenHash);
        return new LoginResponse(token, expiresAt);
    }

    @Transactional
    public void revoke(String authorization) {
        if (authorization == null) {
            return;
        }
        if (!authorization.matches("Bearer [A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization format");
        }
        jdbc.update("DELETE FROM app_session WHERE token_hash = ?", TokenSupport.hash(authorization.substring(7)));
    }
}
