package xyz.yychainsaw.campus_treehole.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.yychainsaw.campus_treehole.dto.LikeStatusResponse;
import xyz.yychainsaw.campus_treehole.repository.PostLikeRepository;

@Service
public class PostLikeService {
    private final PostLikeRepository likes;

    public PostLikeService(PostLikeRepository likes) {
        this.likes = likes;
    }

    @Transactional
    public LikeStatusResponse setLiked(long postId, long userId, boolean liked) {
        if (postId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be positive");
        }
        if (!likes.lockPost(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
        if (liked) {
            likes.add(postId, userId);
        } else {
            likes.remove(postId, userId);
        }
        return likes.findStatuses(List.of(postId), userId)
                .getOrDefault(postId, LikeStatusResponse.EMPTY);
    }
}
