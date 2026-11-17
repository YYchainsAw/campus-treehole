package xyz.yychainsaw.campus_treehole.controller;

import xyz.yychainsaw.campus_treehole.service.CommentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import xyz.yychainsaw.campus_treehole.dto.CreateCommentRequest;
import xyz.yychainsaw.campus_treehole.dto.CommentPage;
import xyz.yychainsaw.campus_treehole.dto.CommentResponse;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService comments;
    private final CurrentUserResolver currentUser;

    public CommentController(CommentService comments, CurrentUserResolver currentUser) {
        this.comments = comments;
        this.currentUser = currentUser;
    }

    @GetMapping
    public CommentPage list(@PathVariable long postId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        if (postId <= 0 || page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid post ID or pagination");
        }
        return comments.list(postId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable long postId,
                                  @Valid @RequestBody CreateCommentRequest body,
                                  HttpServletRequest request) {
        if (postId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid post ID");
        return comments.create(postId, currentUser.required(request), body.content());
    }

}
