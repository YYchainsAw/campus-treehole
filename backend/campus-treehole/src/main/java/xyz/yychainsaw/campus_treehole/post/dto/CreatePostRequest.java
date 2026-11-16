package xyz.yychainsaw.campus_treehole.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(@NotBlank @Size(max = 1000) String content) {
    public CreatePostRequest {
        content = content == null ? null : content.strip();
    }
}
