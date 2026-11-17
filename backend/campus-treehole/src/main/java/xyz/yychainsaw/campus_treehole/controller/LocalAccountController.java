package xyz.yychainsaw.campus_treehole.controller;

import xyz.yychainsaw.campus_treehole.service.LocalAccountService;
import xyz.yychainsaw.campus_treehole.dto.Credentials;
import xyz.yychainsaw.campus_treehole.dto.LoginResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("local")
@RequestMapping("/api/auth/local")
public class LocalAccountController {
    private final LocalAccountService accounts;

    public LocalAccountController(LocalAccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody Credentials credentials) {
        validate(credentials);
        accounts.register(credentials.username(), credentials.password());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody Credentials credentials) {
        validate(credentials);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(accounts.login(credentials.username(), credentials.password()));
    }

    private static void validate(Credentials credentials) {
        if (credentials.username() == null || !credentials.username().matches("[A-Za-z0-9_]{3,32}")
                || credentials.password() == null || credentials.password().length() < 6
                || credentials.password().length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or password format");
        }
    }

}
