package xyz.yychainsaw.campus_treehole.auth;

import xyz.yychainsaw.campus_treehole.config.CurrentUserResolver;
import xyz.yychainsaw.campus_treehole.util.TokenSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class LogoutIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CurrentUserResolver resolver;

    @Test
    void logoutRevokesOnlyTheSelectedSessionAndIsIdempotent() throws Exception {
        String first = TokenSupport.newToken();
        String second = TokenSupport.newToken();
        addSession(first, false);
        addSession(second, false);

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        assertEquals(0, count(first));
        assertEquals(1, count(second));
        MockHttpServletRequest revoked = new MockHttpServletRequest();
        revoked.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + first);
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> resolver.required(revoked)).getStatusCode().value());
        MockHttpServletRequest retained = new MockHttpServletRequest();
        retained.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + second);
        assertEquals(1L, resolver.required(retained));
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
                .andExpect(status().isNoContent());
    }

    @Test
    void absentAndExpiredTokensAreSafeAndMalformedHeadersAreRejected() throws Exception {
        String expired = TokenSupport.newToken();
        String retained = TokenSupport.newToken();
        addSession(expired, true);
        addSession(retained, false);
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
        assertEquals(1, count(retained));
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isBadRequest());
        assertEquals(1, count(retained));
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isNoContent());
        assertEquals(0, count(expired));
        assertEquals(1, count(retained));
        assertEquals(1L, resolver.required(new MockHttpServletRequest()));
    }

    private void addSession(String token, boolean expired) {
        String expiry = expired ? "DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY)" : "DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 1 DAY)";
        jdbc.update("INSERT INTO app_session (token_hash, user_id, expires_at) VALUES (?, 1, " + expiry + ")", TokenSupport.hash(token));
    }

    private int count(String token) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_session WHERE token_hash = ?", Integer.class, TokenSupport.hash(token));
    }
}
