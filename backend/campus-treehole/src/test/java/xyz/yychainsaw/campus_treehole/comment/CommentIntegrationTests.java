package xyz.yychainsaw.campus_treehole.comment;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class CommentIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void createAndListCommentsWithPaginationAndCascade() throws Exception {
        String marker = "comment-test-" + UUID.randomUUID();
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (1, ?, CURRENT_TIMESTAMP(6))", marker);
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE content = ?", Long.class, marker);

        mvc.perform(post("/api/posts/{id}/comments", postId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\" first comment \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("first comment"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").doesNotExist());
        mvc.perform(post("/api/posts/{id}/comments", postId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"second comment\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/posts/{id}/comments", postId).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].content").value("second comment"));
        mvc.perform(get("/api/posts/{id}/comments", postId).param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].content").value("first comment"));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM post_comment WHERE post_id = ? AND user_id = 1", Integer.class, postId));
        jdbc.update("DELETE FROM post WHERE id = ?", postId);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM post_comment WHERE post_id = ?", Integer.class, postId));
    }

    @Test
    void validatesContentAndMissingPosts() throws Exception {
        mvc.perform(get("/api/posts/{id}/comments", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post("/api/posts/{id}/comments", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"text\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/posts/{id}/comments", 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/posts/{id}/comments", 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts/{id}/comments", 1).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts/{id}/comments", 1).param("size", "101")).andExpect(status().isBadRequest());
    }
}
