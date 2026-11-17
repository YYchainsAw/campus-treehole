package xyz.yychainsaw.campus_treehole.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.yychainsaw.campus_treehole.dto.LoginResponse;

@Service
@Profile("local")
public class LocalAccountService {
    private static final int ITERATIONS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] DUMMY_SALT = new byte[16];
    private static final byte[] DUMMY_HASH = new byte[32];
    private final JdbcTemplate jdbc;
    private final AuthSessionService sessions;

    public LocalAccountService(JdbcTemplate jdbc, AuthSessionService sessions) {
        this.jdbc = jdbc;
        this.sessions = sessions;
    }

    @Transactional
    public void register(String username, String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = hash(password, salt);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> connection.prepareStatement("INSERT INTO app_user () VALUES ()", Statement.RETURN_GENERATED_KEYS), keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("User ID was not generated");
        try {
            jdbc.update("INSERT INTO local_account (user_id, username, password_salt, password_hash) VALUES (?, ?, ?, ?)",
                    key.longValue(), username, salt, hash);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        List<Account> accounts = jdbc.query("SELECT user_id, password_salt, password_hash FROM local_account WHERE username = ?",
                (rs, row) -> new Account(rs.getLong(1), rs.getBytes(2), rs.getBytes(3)), username);
        Account account = accounts.isEmpty() ? new Account(0, DUMMY_SALT, DUMMY_HASH) : accounts.get(0);
        if (!MessageDigest.isEqual(hash(password, account.salt()), account.hash()) || accounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return sessions.createSessionForUser(account.userId());
    }

    private static byte[] hash(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private record Account(long userId, byte[] salt, byte[] hash) { }
}
