package xyz.yychainsaw.campus_treehole.post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PostLikeConcurrencyIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void concurrentLikesFromSameUserCreateOnlyOneRecord() throws Exception {
        String marker = "concurrent-like-test-" + UUID.randomUUID();
        jdbc.update("INSERT INTO post (user_id, content, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))", 1L, marker);
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE content = ?", Long.class, marker);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> responses = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                responses.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                    return mvc.perform(post("/api/posts/{id}/likes", postId)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> response : responses) {
                assertEquals(200, response.get(30, TimeUnit.SECONDS));
            }
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM post_like WHERE post_id = ? AND user_id = ?", Integer.class, postId, 1L));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Concurrent requests did not stop");
            // Only remove the exact unique test fixture; ON DELETE CASCADE removes its likes.
            jdbc.update("DELETE FROM post WHERE id = ? AND content = ?", postId, marker);
        }
    }
}
