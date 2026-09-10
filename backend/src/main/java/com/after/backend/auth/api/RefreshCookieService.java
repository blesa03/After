package com.after.backend.auth.api;

import com.after.backend.auth.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshCookieService {

    private final AuthProperties properties;

    public RefreshCookieService(AuthProperties properties) {
        this.properties = properties;
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie ->
                        properties.refresh()
                                .cookieName()
                                .equals(cookie.getName())
                )
                .map(Cookie::getValue)
                .filter(value ->
                        value != null && !value.isBlank()
                )
                .findFirst();
    }

    public ResponseCookie create(String token) {
        return ResponseCookie
                .from(
                        properties.refresh().cookieName(),
                        token
                )
                .httpOnly(true)
                .secure(properties.refresh().secure())
                .sameSite(properties.refresh().sameSite())
                .path(properties.refresh().path())
                .maxAge(properties.refresh().ttl())
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie
                .from(
                        properties.refresh().cookieName(),
                        ""
                )
                .httpOnly(true)
                .secure(properties.refresh().secure())
                .sameSite(properties.refresh().sameSite())
                .path(properties.refresh().path())
                .maxAge(Duration.ZERO)
                .build();
    }
}