package xyz.yychainsaw.campus_treehole.post;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class PostCategoryIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void createsPostsWithRequestedOrDefaultCategoryAndRejectsInvalid() throws Exception {
        String text = UUID.randomUUID().toString();
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + text + "\",\"category\":\"study\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("study"));
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("other"));
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + text + "\",\"category\":\"unknown\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersByCategoryBeforePaginationAndCountsOnlyMatches() throws Exception {
        String marker = UUID.randomUUID().toString();
        Timestamp newest = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999000));
        jdbc.update("INSERT INTO post (user_id, content, created_at, category) VALUES (?, ?, ?, ?)", 1L, marker, newest, "study");
        mvc.perform(get("/api/posts").param("category", "study").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value(marker))
                .andExpect(jsonPath("$.items[0].category").value("study"))
                .andExpect(jsonPath("$.total").value(jdbc.queryForObject("SELECT COUNT(*) FROM post WHERE category = 'study'", Long.class)));
        mvc.perform(get("/api/posts").param("category", "unknown"))
                .andExpect(status().isBadRequest());
    }
}
