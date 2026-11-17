package xyz.yychainsaw.campus_treehole.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpWechatClient implements WechatClient {
    private final String appId;
    private final String appSecret;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public HttpWechatClient(@Value("${WECHAT_APP_ID:}") String appId,
                     @Value("${WECHAT_APP_SECRET:}") String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public WechatIdentity exchange(String code) {
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WeChat login is not configured");
        }
        if (!appId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WeChat login is not configured");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + encode(appId)
                + "&secret=" + encode(appSecret) + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw upstreamUnavailable();
            }
            JsonNode body = json.readTree(response.body());
            JsonNode error = body.get("errcode");
            if (error != null && error.asInt() != 0) {
                if (error.asInt() == 40029 || error.asInt() == 40163) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WeChat login code is invalid or expired");
                }
                throw upstreamUnavailable();
            }
            JsonNode openId = body.get("openid");
            JsonNode sessionKey = body.get("session_key");
            if (openId == null || !openId.isTextual() || !openId.asString().matches("[A-Za-z0-9_-]{1,128}")
                    || sessionKey == null || !sessionKey.isTextual() || sessionKey.asString().isBlank()) {
                throw upstreamUnavailable();
            }
            return new WechatIdentity(appId, openId.asString());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw upstreamUnavailable();
        } catch (Exception exception) {
            // Never expose/log the request URL: it contains the AppSecret and code.
            throw upstreamUnavailable();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ResponseStatusException upstreamUnavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WeChat login is temporarily unavailable");
    }
}
