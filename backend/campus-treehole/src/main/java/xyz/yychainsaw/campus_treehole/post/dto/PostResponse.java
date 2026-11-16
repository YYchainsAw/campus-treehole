package xyz.yychainsaw.campus_treehole.post.dto;

import java.time.LocalDateTime;
import xyz.yychainsaw.campus_treehole.post.entity.Post;

public record PostResponse(Long id, String content, LocalDateTime createdAt) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getContent(), post.getCreatedAt());
    }
}
