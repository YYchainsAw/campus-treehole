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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class LocalPostLikesIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void likesAndUnlikesAreIdempotentAndListShowsTheCurrentUsersState() throws Exception {
        long postId = insertPost(1);
        jdbc.update("INSERT INTO post_like (post_id, user_id) VALUES (?, ?)", postId, 2L);

        mvc.perform(post("/api/posts/{id}/likes", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.likedByMe").value(true));
        mvc.perform(post("/api/posts/{id}/likes", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.likedByMe").value(true));
        assertEquals(1, countLikes(postId, 1));

        mvc.perform(get("/api/posts").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likeCount", contains(2)))
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likedByMe", contains(true)));
        mvc.perform(get("/api/posts/mine").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likeCount", contains(2)))
                .andExpect(jsonPath("$.items[?(@.id == " + postId + ")].likedByMe", contains(true)));

        mvc.perform(delete("/api/posts/{id}/likes", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));
        mvc.perform(delete("/api/posts/{id}/likes", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));
        assertEquals(0, countLikes(postId, 1));
        assertEquals(1, countLikes(postId, 2));
    }

    @Test
    void rejectsMissingPostsAndDeletingPostCascadesLikes() throws Exception {
        mvc.perform(post("/api/posts/{id}/likes", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/{id}/likes", Long.MAX_VALUE)).andExpect(status().isNotFound());

        long postId = insertPost(1);
        jdbc.update("INSERT INTO post_like (post_id, user_id) VALUES (?, ?)", postId, 1L);
        jdbc.update("INSERT INTO post_like (post_id, user_id) VALUES (?, ?)", postId, 2L);
        mvc.perform(delete("/api/posts/{id}", postId)).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM post_like WHERE post_id = ?", Integer.class, postId));
    }

    private long insertPost(long author) {
        String content = "likes-test-" + UUID.randomUUID();
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999000));
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, ?)", author, content, timestamp);
        return jdbc.queryForObject("SELECT id FROM post WHERE content = ?", Long.class, content);
    }

    private int countLikes(long postId, long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_like WHERE post_id = ? AND user_id = ?",
                Integer.class, postId, userId);
    }
}
