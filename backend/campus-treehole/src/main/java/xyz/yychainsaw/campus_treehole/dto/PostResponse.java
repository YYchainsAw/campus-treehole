package xyz.yychainsaw.campus_treehole.dto;

import java.time.LocalDateTime;
import xyz.yychainsaw.campus_treehole.entity.Post;

public record PostResponse(Long id, String content, LocalDateTime createdAt, String category, long likeCount, boolean likedByMe) {
    public static PostResponse from(Post post) {
        return from(post, LikeStatusResponse.EMPTY);
    }

    public static PostResponse from(Post post, LikeStatusResponse likeStatus) {
        return new PostResponse(post.getId(), post.getContent(), post.getCreatedAt(), post.getCategory(),
                likeStatus.likeCount(), likeStatus.likedByMe());
    }
}
