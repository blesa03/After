package com.after.backend.auth.api;

import com.after.backend.auth.api.dto.AccessTokenResponse;
import com.after.backend.auth.api.dto.LoginRequest;
import com.after.backend.auth.api.dto.RegisterRequest;
import com.after.backend.auth.api.dto.UserResponse;
import com.after.backend.auth.application.AuthService;
import com.after.backend.auth.application.AuthSession;
import com.after.backend.auth.exception.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;

    public AuthController(
            AuthService authService,
            RefreshCookieService refreshCookieService
    ) {
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        UserResponse.from(
                                authService.register(request)
                        )
                );
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthSession session = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService
                                .create(session.refreshToken())
                                .toString()
                )
                .body(
                        AccessTokenResponse.from(
                                session.accessToken()
                        )
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            HttpServletRequest request
    ) {
        String rawRefreshToken = refreshCookieService
                .read(request)
                .orElseThrow(InvalidRefreshTokenException::new);

        AuthSession session =
                authService.refresh(rawRefreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService
                                .create(session.refreshToken())
                                .toString()
                )
                .body(
                        AccessTokenResponse.from(
                                session.accessToken()
                        )
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {
        refreshCookieService
                .read(request)
                .ifPresent(authService::logout);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService.clear().toString()
                )
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return UserResponse.from(
                authService.currentUser(jwt.getSubject())
        );
    }
}