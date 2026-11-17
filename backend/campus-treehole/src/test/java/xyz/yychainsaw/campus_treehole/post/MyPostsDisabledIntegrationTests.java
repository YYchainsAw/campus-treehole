package xyz.yychainsaw.campus_treehole.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyPostsDisabledIntegrationTests {
    @Autowired private MockMvc mvc;

    @Test
    void personalOperationsAreUnavailableOutsideLocalProfile() throws Exception {
        mvc.perform(get("/api/posts/mine")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/posts/1")).andExpect(status().isUnauthorized());
    }
}
