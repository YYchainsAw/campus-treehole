package xyz.yychainsaw.campus_treehole.controller;

import xyz.yychainsaw.campus_treehole.service.AuthService;
import xyz.yychainsaw.campus_treehole.dto.LoginRequest;
import xyz.yychainsaw.campus_treehole.dto.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/wechat")
    public ResponseEntity<LoginResponse> wechat(@RequestBody LoginRequest request) {
        if (request.code() == null || request.code().isBlank() || request.code().length() > 512) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login code is required");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(request.code()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout(request.getHeader("Authorization"));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

}
