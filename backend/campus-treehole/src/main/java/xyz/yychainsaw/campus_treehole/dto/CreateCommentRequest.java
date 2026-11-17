package xyz.yychainsaw.campus_treehole.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(@NotBlank @Size(max = 500) String content) {
    public CreateCommentRequest {
        content = content == null ? null : content.strip();
    }
}
