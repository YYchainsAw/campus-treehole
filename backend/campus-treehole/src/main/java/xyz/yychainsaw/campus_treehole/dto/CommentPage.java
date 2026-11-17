package xyz.yychainsaw.campus_treehole.dto;

import java.util.List;

public record CommentPage(List<CommentResponse> items, int page, int size, long total) { }
