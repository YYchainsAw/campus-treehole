package xyz.yychainsaw.campus_treehole.post.controller;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import xyz.yychainsaw.campus_treehole.post.dto.CreatePostRequest;
import xyz.yychainsaw.campus_treehole.post.dto.PostPageResponse;
import xyz.yychainsaw.campus_treehole.post.dto.PostResponse;
import xyz.yychainsaw.campus_treehole.post.service.PostService;

/** Temporary operations for local development, until real login is implemented. */
@RestController
@Profile("local")
@RequestMapping("/api/posts")
public class LocalPostController {
    private static final long LOCAL_USER_ID = 1L;
    private final PostService postService;

    public LocalPostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody CreatePostRequest request) {
        return postService.create(LOCAL_USER_ID, request.content());
    }

    @GetMapping("/mine")
    public PostPageResponse mine(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        PostController.validatePagination(page, size);
        return postService.listByUser(LOCAL_USER_ID, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be positive");
        }
        if (!postService.deleteByUser(id, LOCAL_USER_ID)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
    }
}
