package xyz.yychainsaw.campus_treehole.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalAccountDisabledIntegrationTests {
    @Autowired private MockMvc mvc;

    @Test
    void localAccountsAreUnavailableOutsideLocalProfile() throws Exception {
        String credentials = "{\"username\":\"student\",\"password\":\"testpass1\"}";
        mvc.perform(post("/api/auth/local/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/local/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isNotFound());
    }
}
