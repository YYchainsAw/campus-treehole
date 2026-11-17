package xyz.yychainsaw.campus_treehole.config;

import xyz.yychainsaw.campus_treehole.util.TokenSupport;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.OptionalLong;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserResolver {
    private final JdbcTemplate jdbc;
    private final Environment environment;

    public CurrentUserResolver(JdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.environment = environment;
    }

    public OptionalLong optional(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            return environment.acceptsProfiles(Profiles.of("local")) ? OptionalLong.of(1L) : OptionalLong.empty();
        }
        if (!authorization.matches("Bearer [A-Za-z0-9_-]{43}")) {
            throw unauthorized();
        }
        byte[] hash = TokenSupport.hash(authorization.substring(7));
        List<Long> users = jdbc.query("SELECT user_id FROM app_session WHERE token_hash = ? AND expires_at > UTC_TIMESTAMP(6)",
                (rs, row) -> rs.getLong(1), hash);
        if (users.size() != 1) {
            throw unauthorized();
        }
        return OptionalLong.of(users.get(0));
    }

    public long required(HttpServletRequest request) {
        return optional(request).orElseThrow(CurrentUserResolver::unauthorized);
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
    }
}
