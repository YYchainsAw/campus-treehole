package xyz.yychainsaw.campus_treehole.auth;

import xyz.yychainsaw.campus_treehole.config.CurrentUserResolver;
import xyz.yychainsaw.campus_treehole.service.HttpWechatClient;
import xyz.yychainsaw.campus_treehole.service.WechatClient;
import xyz.yychainsaw.campus_treehole.util.TokenSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WechatLoginIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CurrentUserResolver resolver;
    @MockitoBean private WechatClient wechat;

    @Test
    void loginCreatesUserAndHashedSessionAndReusesUserOnSubsequentLogin() throws Exception {
        String openId = "test_" + UUID.randomUUID().toString().replace("-", "");
        when(wechat.exchange(anyString())).thenReturn(new WechatClient.WechatIdentity("test_app", openId));

        String body1 = mvc.perform(post("/api/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code-1\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String body2 = mvc.perform(post("/api/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code-2\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String token1 = JsonPath.read(body1, "$.token");
        String token2 = JsonPath.read(body2, "$.token");
        assertTrue(token1.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(token1, token2);
        long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE wechat_appid = ? AND wechat_openid = ?",
                Long.class, "test_app", openId);
        assertTrue(userId > 1, "WeChat users must not reuse the local identity");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE wechat_appid = ? AND wechat_openid = ?",
                Integer.class, "test_app", openId));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM app_session WHERE user_id = ?", Integer.class, userId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM app_session WHERE token_hash = ? AND user_id = ?",
                Integer.class, TokenSupport.hash(token1), userId));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token1);
        assertEquals(OptionalLong.of(userId), resolver.optional(request));
        jdbc.update("UPDATE app_session SET expires_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND) WHERE token_hash = ?",
                TokenSupport.hash(token1));
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> resolver.required(request)).getStatusCode().value());
    }

    @Test
    void loginRequiresAValidCode() throws Exception {
        mvc.perform(post("/api/auth/wechat").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/wechat").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRealWechatConfigurationReturnsUnavailableWithoutNetworkRequest() {
        HttpWechatClient client = new HttpWechatClient("", "");
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> client.exchange("code")).getStatusCode().value());
    }
}
