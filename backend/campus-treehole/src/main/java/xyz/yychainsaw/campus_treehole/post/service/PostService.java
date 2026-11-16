package xyz.yychainsaw.campus_treehole.post.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.yychainsaw.campus_treehole.post.dto.PostPageResponse;
import xyz.yychainsaw.campus_treehole.post.dto.PostResponse;
import xyz.yychainsaw.campus_treehole.post.entity.Post;
import xyz.yychainsaw.campus_treehole.post.repository.PostRepository;

@Service
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public PostPageResponse list(int page, int size) {
        Page<Post> results = postRepository.findAll(pageRequest(page, size));
        return toResponse(results);
    }

    @Transactional(readOnly = true)
    public PostPageResponse listByUser(long userId, int page, int size) {
        Page<Post> results = postRepository.findByUserId(userId, pageRequest(page, size));
        return toResponse(results);
    }

    private PageRequest pageRequest(int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return PageRequest.of(page, size, sort);
    }

    private PostPageResponse toResponse(Page<Post> results) {
        return new PostPageResponse(
                results.getContent().stream().map(PostResponse::from).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements());
    }

    @Transactional
    public PostResponse create(long userId, String content) {
        Post post = new Post(userId, content, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public boolean deleteByUser(long id, long userId) {
        return postRepository.deleteOwnedPost(id, userId) == 1;
    }
}
