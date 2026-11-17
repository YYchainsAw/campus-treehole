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
class PostLikesDisabledIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void likesAreNotWritableOutsideLocalAndPublicListDoesNotAssumeTestIdentity() throws Exception {
        String content = "likes-public-" + UUID.randomUUID();
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999000));
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, ?)", 1L, content, timestamp);
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE content = ?", Long.class, content);
        jdbc.update("INSERT INTO post_like (post_id, user_id) VALUES (?, ?)", postId, 1L);

        mvc.perform(post("/api/posts/{id}/likes", postId)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/posts/{id}/likes", postId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likeCount", contains(1)))
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likedByMe", contains(false)));
    }
}
