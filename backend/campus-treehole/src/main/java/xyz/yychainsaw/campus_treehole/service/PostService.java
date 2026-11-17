package xyz.yychainsaw.campus_treehole.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.yychainsaw.campus_treehole.dto.LikeStatusResponse;
import xyz.yychainsaw.campus_treehole.dto.PostPageResponse;
import xyz.yychainsaw.campus_treehole.dto.PostResponse;
import xyz.yychainsaw.campus_treehole.entity.Post;
import xyz.yychainsaw.campus_treehole.repository.PostRepository;
import xyz.yychainsaw.campus_treehole.repository.PostLikeRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    public PostService(PostRepository postRepository, PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
    }

    @Transactional(readOnly = true)
    public PostPageResponse list(int page, int size, Long viewerId, String category) {
        Page<Post> results = category == null ? postRepository.findAll(pageRequest(page, size))
                : postRepository.findByCategory(category, pageRequest(page, size));
        return toResponse(results, viewerId);
    }

    @Transactional(readOnly = true)
    public PostPageResponse listByUser(long userId, int page, int size) {
        Page<Post> results = postRepository.findByUserId(userId, pageRequest(page, size));
        return toResponse(results, userId);
    }

    private PageRequest pageRequest(int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return PageRequest.of(page, size, sort);
    }

    private PostPageResponse toResponse(Page<Post> results, Long viewerId) {
        List<Long> ids = results.getContent().stream().map(Post::getId).toList();
        Map<Long, LikeStatusResponse> statuses = postLikeRepository.findStatuses(ids, viewerId);
        return new PostPageResponse(
                results.getContent().stream()
                        .map(post -> PostResponse.from(post, statuses.getOrDefault(post.getId(), LikeStatusResponse.EMPTY)))
                        .toList(),
                results.getNumber(), results.getSize(), results.getTotalElements());
    }

    @Transactional
    public PostResponse create(long userId, String content, String category) {
        Post post = new Post(userId, content, category, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public boolean deleteByUser(long id, long userId) {
        return postRepository.deleteOwnedPost(id, userId) == 1;
    }
}
