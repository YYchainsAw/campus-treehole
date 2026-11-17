package xyz.yychainsaw.campus_treehole.auth;

import xyz.yychainsaw.campus_treehole.util.TokenSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class LocalAccountIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void registrationStoresHashAndLoginCreatesSession() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String credentials = "{\"username\":\"" + username + "\",\"password\":\"testpass1\"}";
        mvc.perform(post("/api/auth/local/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        long userId = jdbc.queryForObject("SELECT user_id FROM local_account WHERE username = ?", Long.class, username);
        assertTrue(userId > 1);
        byte[] hash = jdbc.queryForObject("SELECT password_hash FROM local_account WHERE username = ?", byte[].class, username);
        assertEquals(32, hash.length);
        assertNotEquals("testpass1", new String(hash, java.nio.charset.StandardCharsets.UTF_8));
        String body = mvc.perform(post("/api/auth/local/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM app_session WHERE token_hash = ? AND user_id = ?",
                Integer.class, TokenSupport.hash(token), userId));
        mvc.perform(post("/api/auth/local/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesInputsAndRejectsDuplicateNames() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String credentials = "{\"username\":\"" + username + "\",\"password\":\"testpass1\"}";
        mvc.perform(post("/api/auth/local/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/local/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/local/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"tiny\"}"))
                .andExpect(status().isBadRequest());
    }
}
