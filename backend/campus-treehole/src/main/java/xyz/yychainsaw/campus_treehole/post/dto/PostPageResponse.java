package xyz.yychainsaw.campus_treehole.post.dto;

import java.util.List;

public record PostPageResponse(List<PostResponse> items, int page, int size, long total) {
}
