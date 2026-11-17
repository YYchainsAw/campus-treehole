package xyz.yychainsaw.campus_treehole.dto;

public record LikeStatusResponse(long likeCount, boolean likedByMe) {
    public static final LikeStatusResponse EMPTY = new LikeStatusResponse(0, false);
}
