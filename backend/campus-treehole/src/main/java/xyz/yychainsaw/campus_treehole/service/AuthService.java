package xyz.yychainsaw.campus_treehole.service;

import org.springframework.stereotype.Service;
import xyz.yychainsaw.campus_treehole.dto.LoginResponse;

@Service
public class AuthService {
    private final WechatClient wechat;
    private final AuthSessionService sessions;

    public AuthService(WechatClient wechat, AuthSessionService sessions) {
        this.wechat = wechat;
        this.sessions = sessions;
    }

    public LoginResponse login(String code) {
        // Network calls must not keep a database transaction/connection open.
        WechatClient.WechatIdentity identity = wechat.exchange(code);
        return sessions.createSession(identity);
    }

    public void logout(String authorization) {
        sessions.revoke(authorization);
    }

}
