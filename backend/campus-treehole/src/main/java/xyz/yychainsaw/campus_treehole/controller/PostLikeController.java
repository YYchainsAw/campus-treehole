package xyz.yychainsaw.campus_treehole.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.yychainsaw.campus_treehole.config.CurrentUserResolver;
import xyz.yychainsaw.campus_treehole.dto.LikeStatusResponse;
import xyz.yychainsaw.campus_treehole.service.PostLikeService;

@RestController
@RequestMapping("/api/posts/{id}/likes")
public class PostLikeController {
    private final PostLikeService likes;
    private final CurrentUserResolver currentUser;

    public PostLikeController(PostLikeService likes, CurrentUserResolver currentUser) {
        this.likes = likes;
        this.currentUser = currentUser;
    }

    @PostMapping
    public LikeStatusResponse like(@PathVariable long id, HttpServletRequest request) {
        return likes.setLiked(id, currentUser.required(request), true);
    }

    @DeleteMapping
    public LikeStatusResponse unlike(@PathVariable long id, HttpServletRequest request) {
        return likes.setLiked(id, currentUser.required(request), false);
    }
}
