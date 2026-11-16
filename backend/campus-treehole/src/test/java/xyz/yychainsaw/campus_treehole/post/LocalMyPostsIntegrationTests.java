package xyz.yychainsaw.campus_treehole.post;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class LocalMyPostsIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void showsOnlyOwnPostsWithPagination() throws Exception {
        String marker = UUID.randomUUID().toString();
        long first = insertPost(1, "first-" + marker);
        long second = insertPost(1, "second-" + marker);
        insertPost(2, "other-" + marker);

        mvc.perform(get("/api/posts/mine").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(second))
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.page").value(0));
        mvc.perform(get("/api/posts/mine").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(first));
        mvc.perform(get("/api/posts/mine").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts/mine").param("page", "2147483647").param("size", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesOnlyOwnPostAndDoesNotRevealOtherPosts() throws Exception {
        String marker = UUID.randomUUID().toString();
        long own = insertPost(1, "own-" + marker);
        long other = insertPost(2, "other-" + marker);

        mvc.perform(delete("/api/posts/{id}", other)).andExpect(status().isNotFound());
        assertEquals(1, countPost(other));
        mvc.perform(delete("/api/posts/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/{id}", 0)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/posts/{id}", "invalid")).andExpect(status().isBadRequest());

        mvc.perform(delete("/api/posts/{id}", own)).andExpect(status().isNoContent());
        assertEquals(0, countPost(own));
        mvc.perform(delete("/api/posts/{id}", own)).andExpect(status().isNotFound());
    }

    private long insertPost(long userId, String content) {
        Timestamp time = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999000));
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, ?)", userId, content, time);
        return jdbc.queryForObject("SELECT id FROM post WHERE content = ?", Long.class, content);
    }

    private int countPost(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post WHERE id = ?", Integer.class, id);
    }
}
