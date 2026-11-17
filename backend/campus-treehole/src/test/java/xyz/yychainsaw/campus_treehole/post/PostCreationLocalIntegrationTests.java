package xyz.yychainsaw.campus_treehole.post;

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
import org.springframework.test.web.servlet.MvcResult;
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
class PostCreationLocalIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void createsTrimmedPostAndShowsItInTheList() throws Exception {
        String content = "integration-" + UUID.randomUUID();
        MvcResult created = mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  " + content + "  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andReturn();

        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Long author = jdbc.queryForObject("SELECT user_id FROM post WHERE id = ?", Long.class, id.longValue());
        assertEquals(1L, author);

        mvc.perform(get("/api/posts").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + id.longValue() + ")].content").value(content));
    }

    @Test
    void rejectsMissingBlankAndOversizedContent() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
