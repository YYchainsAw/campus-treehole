package xyz.yychainsaw.campus_treehole.post;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listsNewestPostsFirstAndPagesWithoutExposingAuthors() throws Exception {
        String marker = UUID.randomUUID().toString();
        String first = "first-" + marker;
        String second = "second-" + marker;
        Timestamp sameTime = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999000));

        // Equal timestamps exercise the ID tie-breaker. The transaction rolls both rows back.
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, ?)", 1L, first, sameTime);
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, ?)", 2L, second, sameTime);

        mvc.perform(get("/api/posts").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.items[0].content").value(second))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].userId").doesNotExist());

        mvc.perform(get("/api/posts").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].content").value(first));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mvc.perform(get("/api/posts").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts").param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts").param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts").param("page", "not-a-number"))
                .andExpect(status().isBadRequest());
    }
}
