package com.after.backend.auth.application;

import com.after.backend.auth.api.dto.LoginRequest;
import com.after.backend.auth.api.dto.RegisterRequest;
import com.after.backend.auth.exception.DuplicateEmailException;
import com.after.backend.auth.exception.InvalidAccessTokenException;
import com.after.backend.auth.exception.InvalidCredentialsException;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(email, passwordHash);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String email = User.normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        AccessToken accessToken =
                jwtService.issueAccessToken(user);

        IssuedRefreshToken refreshToken =
                refreshTokenService.issue(user);

        return new AuthSession(
                accessToken,
                refreshToken.value()
        );
    }

    @Transactional
    public AuthSession refresh(String rawRefreshToken) {
        IssuedRefreshToken refreshToken =
                refreshTokenService.rotate(rawRefreshToken);

        AccessToken accessToken =
                jwtService.issueAccessToken(refreshToken.user());

        return new AuthSession(
                accessToken,
                refreshToken.value()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public User currentUser(String subject) {
        UUID userId;

        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }

        return userRepository.findById(userId)
                .orElseThrow(InvalidAccessTokenException::new);
    }
}