package xyz.yychainsaw.campus_treehole.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.yychainsaw.campus_treehole.dto.LikeStatusResponse;

@Repository
public class PostLikeRepository {
    private final JdbcTemplate jdbc;

    public PostLikeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Read all counts in one query, independently of the number of posts on the page. */
    public Map<Long, LikeStatusResponse> findStatuses(List<Long> postIds, Long viewerId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = postIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT post_id, COUNT(*) AS like_count, "
                + "MAX(CASE WHEN user_id = ? THEN 1 ELSE 0 END) AS liked_by_me "
                + "FROM post_like WHERE post_id IN (" + placeholders + ") GROUP BY post_id";
        Object[] parameters = new Object[postIds.size() + 1];
        // SQL NULL makes every comparison false for unauthenticated viewers.
        parameters[0] = viewerId;
        for (int i = 0; i < postIds.size(); i++) {
            parameters[i + 1] = postIds.get(i);
        }
        Map<Long, LikeStatusResponse> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getLong("post_id"), new LikeStatusResponse(
                    rs.getLong("like_count"), rs.getBoolean("liked_by_me")));
        }, parameters);
        return result;
    }

    /** Lock the post so likes, unlikes and post deletion cannot race. */
    public boolean lockPost(long postId) {
        return !jdbc.queryForList("SELECT id FROM post WHERE id = ? FOR UPDATE", Long.class, postId).isEmpty();
    }

    public void add(long postId, long userId) {
        jdbc.update("INSERT INTO post_like (post_id, user_id) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE post_id = post_id", postId, userId);
    }

    public void remove(long postId, long userId) {
        jdbc.update("DELETE FROM post_like WHERE post_id = ? AND user_id = ?", postId, userId);
    }
}
