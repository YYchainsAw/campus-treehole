package xyz.yychainsaw.campus_treehole.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import xyz.yychainsaw.campus_treehole.config.CurrentUserResolver;
import xyz.yychainsaw.campus_treehole.dto.PostPageResponse;
import xyz.yychainsaw.campus_treehole.dto.PostCategory;
import xyz.yychainsaw.campus_treehole.service.PostService;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final CurrentUserResolver currentUser;

    public PostController(PostService postService, CurrentUserResolver currentUser) {
        this.postService = postService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PostPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            HttpServletRequest request) {
        validatePagination(page, size);
        PostCategory.validate(category);
        var viewer = currentUser.optional(request);
        return postService.list(page, size, viewer.isPresent() ? viewer.getAsLong() : null, category);
    }

    static void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative, size must be between 1 and 100, and the offset must be within range");
        }
    }
}
