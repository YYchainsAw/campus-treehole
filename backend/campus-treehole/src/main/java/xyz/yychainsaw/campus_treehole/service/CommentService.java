package xyz.yychainsaw.campus_treehole.service;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.yychainsaw.campus_treehole.dto.CommentPage;
import xyz.yychainsaw.campus_treehole.dto.CommentResponse;

@Service
public class CommentService {
    private final JdbcTemplate jdbc;

    public CommentService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public CommentPage list(long postId, int page, int size) {
        if (jdbc.queryForList("SELECT id FROM post WHERE id = ?", Long.class, postId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM post_comment WHERE post_id = ?", Long.class, postId);
        List<CommentResponse> items = jdbc.query(
                "SELECT id, content, created_at FROM post_comment WHERE post_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new CommentResponse(rs.getLong("id"), rs.getString("content"), rs.getTimestamp("created_at").toLocalDateTime()),
                postId, size, (long) page * size);
        return new CommentPage(items, page, size, total);
    }

    @Transactional
    public CommentResponse create(long postId, long userId, String content) {
        if (jdbc.queryForList("SELECT id FROM post WHERE id = ? FOR UPDATE", Long.class, postId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO post_comment (post_id, user_id, content, created_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, postId);
            statement.setLong(2, userId);
            statement.setString(3, content);
            statement.setTimestamp(4, Timestamp.valueOf(createdAt));
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("Comment ID was not generated");
        return new CommentResponse(key.longValue(), content, createdAt);
    }
}
