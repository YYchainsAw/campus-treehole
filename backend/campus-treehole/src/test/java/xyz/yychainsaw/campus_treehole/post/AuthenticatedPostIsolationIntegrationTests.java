package xyz.yychainsaw.campus_treehole.post;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticatedPostIsolationIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void authenticatedUsersSeeTheirOwnPostsAndLikesAndCannotDeleteOthers() throws Exception {
        long alice = createUser();
        long bob = createUser();
        String aliceToken = createSession(alice);
        String bobToken = createSession(bob);
        String marker = "auth-isolation-" + UUID.randomUUID();
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, '9999-12-31 23:59:59.999999')", alice, marker);
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE content = ?", Long.class, marker);

        mvc.perform(post("/api/posts/{id}/likes", postId).header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likedByMe").value(true));
        mvc.perform(get("/api/posts").param("size", "100").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likedByMe", contains(true)));
        mvc.perform(get("/api/posts").param("size", "100").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likedByMe", contains(false)));
        mvc.perform(get("/api/posts/mine").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(postId));
        mvc.perform(get("/api/posts/mine").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(delete("/api/posts/{id}", postId).header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/{id}", postId).header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidAndMissingCredentialsCannotMutatePosts() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts/mine").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    private long createUser() {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app_user (wechat_appid, wechat_openid) VALUES (NULL, NULL)",
                    Statement.RETURN_GENERATED_KEYS);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    private String createSession(long userId) throws Exception {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        jdbc.update("INSERT INTO app_session (token_hash, user_id, expires_at) VALUES (?, ?, DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 1 HOUR))", hash, userId);
        return token;
    }
}
