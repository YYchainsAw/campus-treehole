package xyz.yychainsaw.campus_treehole.service;

public interface WechatClient {
    WechatIdentity exchange(String code);

    record WechatIdentity(String appId, String openId) {
    }
}
