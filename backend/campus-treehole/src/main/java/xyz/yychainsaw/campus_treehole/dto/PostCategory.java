package xyz.yychainsaw.campus_treehole.dto;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Values shared by post creation and optional list filters. */
public final class PostCategory {
    private static final Set<String> VALUES = Set.of("study", "life", "mood", "help", "other");

    private PostCategory() { }

    public static String validate(String value) {
        if (value != null && !VALUES.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid post category");
        }
        return value;
    }
}
