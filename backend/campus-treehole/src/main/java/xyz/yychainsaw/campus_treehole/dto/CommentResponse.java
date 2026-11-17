package xyz.yychainsaw.campus_treehole.dto;

import java.time.LocalDateTime;

public record CommentResponse(long id, String content, LocalDateTime createdAt) { }
