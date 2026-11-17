package xyz.yychainsaw.campus_treehole.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(@NotBlank @Size(max = 1000) String content, String category) {
    public CreatePostRequest {
        content = content == null ? null : content.strip();
        category = category == null ? "other" : PostCategory.validate(category);
    }
}
