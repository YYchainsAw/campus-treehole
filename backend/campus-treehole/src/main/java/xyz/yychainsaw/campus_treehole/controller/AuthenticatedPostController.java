package xyz.yychainsaw.campus_treehole.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
import xyz.yychainsaw.campus_treehole.config.CurrentUserResolver;
import xyz.yychainsaw.campus_treehole.dto.CreatePostRequest;
import xyz.yychainsaw.campus_treehole.dto.PostPageResponse;
import xyz.yychainsaw.campus_treehole.dto.PostResponse;
import xyz.yychainsaw.campus_treehole.service.PostService;

@RestController
@RequestMapping("/api/posts")
public class AuthenticatedPostController {
    private final PostService postService;
    private final CurrentUserResolver currentUser;

    public AuthenticatedPostController(PostService postService, CurrentUserResolver currentUser) {
        this.postService = postService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody CreatePostRequest request, HttpServletRequest httpRequest) {
        return postService.create(currentUser.required(httpRequest), request.content(), request.category());
    }

    @GetMapping("/mine")
    public PostPageResponse mine(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 HttpServletRequest request) {
        long userId = currentUser.required(request);
        PostController.validatePagination(page, size);
        return postService.listByUser(userId, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, HttpServletRequest request) {
        long userId = currentUser.required(request);
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be positive");
        }
        if (!postService.deleteByUser(id, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
    }
}
